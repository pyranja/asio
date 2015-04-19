package at.ac.univie.isc.asio.platform;

import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(HierarchicalContextRunner.class)
public class LauncherTest {

  private final Daemon daemon = Mockito.mock(Daemon.class);
  private final Launcher.Companion companion = Mockito.mock(Launcher.Companion.class);
  private final Launcher subject = new Launcher(daemon, companion);

  @Test(expected = Launcher.StartupFailed.class)
  public void should_wrap_exception_from_daemon_lib() throws Exception {
    given(daemon.isDaemonized()).willThrow(new RuntimeException());
    subject.daemonize();
  }

  @Test(expected = Launcher.StartupFailed.class)
  public void should_wrap_exception_from_companion() throws Exception {
    given(companion.shouldDaemonize()).willThrow(new RuntimeException());
    subject.daemonize();
  }

  @Test
  public void should_log_failure() throws Exception {
    final RuntimeException failure = new RuntimeException();
    given(companion.shouldDaemonize()).willThrow(failure);
    try { subject.daemonize(); } catch (final Exception ignored) {}
    verify(companion).log("failure - %s", failure);
  }

  public class ParentAndDaemonDisabled {
    @Test
    public void should_do_nothing() throws Exception {
      subject.daemonize();
      verify(daemon).isDaemonized();
      verifyNoMoreInteractions(daemon);
    }

    @Test
    public void should_log_skipping() throws Exception {
      subject.daemonize();
      verify(companion).log("daemon not enabled");
    }
  }

  public class ForkedChild {
    @Before
    public void mocks() {
      given(daemon.isDaemonized()).willReturn(true);
    }

    @Test
    public void should_detach() throws Exception {
      subject.daemonize();
      verify(daemon).init(null);
    }

    @Test
    public void should_init_with_provided_pid_file_path() throws Exception {
      final String expected = "/path/to/my.pid";
      given(companion.pidfile()).willReturn(expected);
      subject.daemonize();
      verify(daemon).init(expected);
    }

    @Test
    public void should_log_detaching() throws Exception {
      subject.daemonize();
      verify(companion).log("detaching daemon process (pid file : %s)", null);
    }
  }

  public class ParentAndDaemonEnabled {
    @Before
    public void mocks() {
      given(companion.shouldDaemonize()).willReturn(true);
    }

    @Test
    public void should_exit_with_code__zero__after_forking() throws Exception {
      subject.daemonize();
      verify(companion).exit();
    }

    @Test
    public void should_fork() throws Exception {
      final JavaVMArguments expected = new JavaVMArguments();
      given(companion.arguments()).willReturn(expected);
      subject.daemonize();
      verify(daemon).daemonize(expected);
    }

    @Test
    public void should_log_forking() throws Exception {
      subject.daemonize();
      verify(companion).log("forking daemon process (cli args : %s)", null);
      verify(companion).log("daemon forked - exiting parent");
    }
  }
}
