package at.ac.univie.isc.asio.tool;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ALL")
@RunWith(Enclosed.class)
public class StatefulMonitorTest {
  private static final ExecutorService exec = Executors.newCachedThreadPool();

  @AfterClass
  public static void shutdownExecutor() throws Exception {
    exec.shutdownNow();
  }

  public static class StateInactive {
    @Rule
    public final ExpectedException error = ExpectedException.none();
    @SuppressWarnings("unchecked")
    private final Callable<Void> action = Mockito.mock(Callable.class);

    private final StatefulMonitor subject =
        StatefulMonitor.withMaximalWaitingTime(Timeout.undefined());

    @Test
    public void should_not_be_active() throws Exception {
      assertThat(subject.isActive(), equalTo(false));
    }

    @Test
    public void should_throw_on_activity_check() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.ensureActive();
    }

    @Test
    public void should_execute_callback_on_activate() throws Exception {
      subject.activate(action);
      verify(action).call();
    }

    @Test
    public void should_transition_to_state_active_on_activate() throws Exception {
      subject.activate(StatefulMonitor.noop());
      assertThat("monitor not active", subject.isActive(), equalTo(true));
    }

    @Test
    public void should_not_transition_to_active_if_action_fails() throws Exception {
      given(action.call()).willThrow(new RuntimeException());
      try { subject.activate(action); } catch (Exception e) {}
      assertThat("transitioned to active although activation action failed",
          subject.isActive(), equalTo(false));
    }

    @Test
    public void should_reject_disable_attempt() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.disable(StatefulMonitor.noop());
    }

    @Test
    public void should_stay_inactive_after_disable_attempt() throws Exception {
      try { subject.disable(StatefulMonitor.noop()); } catch (Exception ignored) {}
      assertThat(subject.isActive(), equalTo(false));
    }

    @Test
    public void should_reject_atomic_operation() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.atomic(action);
    }
  }

  public static class StateActive {
    @Rule
    public final ExpectedException error = ExpectedException.none();
    @SuppressWarnings("unchecked")
    private final Callable<Void> action = Mockito.mock(Callable.class);

    private final StatefulMonitor subject =
        StatefulMonitor.withMaximalWaitingTime(Timeout.undefined());

    @Before
    public void activateMonitor() {
      subject.activate(StatefulMonitor.noop());
    }

    @Test
    public void should_be_active() throws Exception {
      assertThat(subject.isActive(), equalTo(true));
    }

    @Test
    public void should_not_fail_when_ensuring_activity() throws Exception {
      subject.ensureActive();
    }

    @Test
    public void should_execute_callback_on_disable() throws Exception {
      subject.disable(action);
      verify(action).call();
    }

    @Test
    public void should_transition_to_state_inactive_on_disable() throws Exception {
      subject.disable(StatefulMonitor.noop());
      assertThat("still active after disabling", subject.isActive(), equalTo(false));
    }

    @Test
    public void should_transition_to_state_inactive_even_if_action_fails() throws Exception {
      given(action.call()).willThrow(new RuntimeException());
      try { subject.disable(action); } catch (Exception ignored) {}
      assertThat("still active after failed disable attempt", subject.isActive(), equalTo(false));
    }

    @Test
    public void should_reject_activation_attempt() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.activate(StatefulMonitor.noop());
    }

    @Test
    public void should_stay_active_after_activation_attempt() throws Exception {
      try { subject.activate(StatefulMonitor.noop()); } catch (Exception ignored) {}
      assertThat("must stay active after failed activation", subject.isActive(), equalTo(true));
    }

    @Test
    public void should_accept_atomic_operation() throws Exception {
      subject.atomic(action);
      verify(action).call();
    }

    @Test
    public void should_yield_result_of_atomic_operation() throws Exception {
      assertThat(subject.atomic(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
          return 23;
        }
      }), equalTo(23));
    }
  }

  public static class RunLockedExpectingState {
    @Rule
    public final ExpectedException error = ExpectedException.none();
    @SuppressWarnings("unchecked")
    private final Callable<Integer> action = Mockito.mock(Callable.class);

    private final StatefulMonitor subject =
        StatefulMonitor.withMaximalWaitingTime(Timeout.undefined());

    @After
    public void verifyMonitorLockReleased() {
      assertThat("still locked after execution", subject.monitor().isHeldByCurrentThread(), equalTo(false));
      assertThat("state changed after execution", subject.isActive(), equalTo(false));
    }

    @Test
    public void should_execute_supplied_callback() throws Exception {
      subject.runLockedExpectingState(StatefulMonitor.INACTIVE, action);
      verify(action).call();
    }

    @Test
    public void should_yield_callback_result() throws Exception {
      given(action.call()).willReturn(43);
      assertThat(subject.runLockedExpectingState(StatefulMonitor.INACTIVE, action), equalTo(43));
    }

    @Test
    public void should_rethrow_unchecked_exception_from_callback() throws Exception {
      given(action.call()).willThrow(new IllegalArgumentException());
      error.expect(IllegalArgumentException.class);
      subject.runLockedExpectingState(StatefulMonitor.INACTIVE, action);
    }

    @Test
    public void should_rethrow_checked_exception_from_callback_wrapped() throws Exception {
      final Exception failure = new Exception();
      given(action.call()).willThrow(failure);
      error.expect(UncheckedExecutionException.class);
      error.expectCause(sameInstance(failure));
      subject.runLockedExpectingState(StatefulMonitor.INACTIVE, action);
    }

    @Test
    public void should_fail_immediately_if_monitor_is_not_in_expected_state() throws Exception {
      error.expect(StatefulMonitor.IllegalMonitorState.class);
      subject.runLockedExpectingState(StatefulMonitor.ACTIVE, action);
    }

    @Test
    public void should_hold_lock_when_executing_action() throws Exception {
      assertThat("locked before execution", subject.monitor().isLocked(), equalTo(false));
      subject.runLockedExpectingState(StatefulMonitor.INACTIVE, new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          assertThat("not locked during action",
              subject.monitor().isHeldByCurrentThread(), equalTo(true));
          return null;
        }
      });
    }

    @Test
    public void should_unlock_after_completion() throws Exception {
      assertThat("locked before execution", subject.monitor().isLocked(), equalTo(false));
      subject.runLockedExpectingState(StatefulMonitor.INACTIVE, StatefulMonitor.noop());
      assertThat("still locked after execution", subject.monitor().isLocked(), equalTo(false));
    }

    @Test
    public void should_release_lock_even_if_action_fails() throws Exception {
      given(action.call()).willThrow(new RuntimeException());
      try {
        subject.runLockedExpectingState(StatefulMonitor.INACTIVE, StatefulMonitor.noop());
      } catch (Exception ignored) {}
    }

    @Test
    public void should_fail_if_already_locked_and_max_waiting_time_exceeded() throws Exception {
      error.expect(UncheckedTimeoutException.class);
      // must lock in other thread as monitor lock is reentrant
      exec.submit(new Runnable() {
        @Override
        public void run() {
          subject.monitor().lock();
        }
      }).get();
      subject.runLockedExpectingState(StatefulMonitor.INACTIVE, action);
    }
  }
}
