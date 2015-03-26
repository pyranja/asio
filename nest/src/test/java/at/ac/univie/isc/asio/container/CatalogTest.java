package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.CaptureEvents;
import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
@RunWith(Enclosed.class)
public class CatalogTest {

  /**
   * Only deploy()/drop() are public interface
   */
  public static class PublicApi {
    @Rule
    public ExpectedException error = ExpectedException.none();

    private Catalog<Container> subject = new Catalog<>(new EventBus(), TimeoutSpec.undefined());

    @Test
    public void should_yield_absent_if_deploying_new_container() throws Exception {
      final Optional<Container> former = subject.deploy(StubContainer.create("test"));
      assertThat(former.isPresent(), is(false));
    }

    @Test
    public void should_yield_former_if_replacing() throws Exception {
      final Container replaced = StubContainer.create("test");
      subject.deploy(replaced);
      final Container replacement = StubContainer.create("test");
      final Optional<Container> former = subject.deploy(replacement);
      assertThat(former.get(), is(replaced));
    }

    @Test
    public void should_store_deployed_container() throws Exception {
      final StubContainer deployed = StubContainer.create("test");
      subject.deploy(deployed);
      assertThat(subject.findAll(), hasItem(deployed));
    }

    @Test
    public void should_yield_absent_if_dropping_missing() throws Exception {
      final Optional<Container> dropped = subject.drop(Schema.valueOf("name"));
      assertThat(dropped.isPresent(), is(false));
    }

    @Test
    public void should_yield_dropped() throws Exception {
      final Container container = StubContainer.create("name");
      subject.deploy(container);
      final Optional<Container> dropped = subject.drop(Schema.valueOf("name"));
      assertThat(dropped.get(), is(container));
    }

    @Test
    public void should_remove_dropped_from_internal_storage() throws Exception {
      final StubContainer dropped = StubContainer.create("test");
      subject.deploy(dropped);
      subject.drop(Schema.valueOf("test"));
      assertThat(subject.findAll(), not(hasItem(dropped)));
    }
  }


  public static class Clear {
    @Rule
    public ExpectedException error = ExpectedException.none();

    private Catalog<Container> subject = new Catalog<>(new EventBus(), TimeoutSpec.undefined());

    @Test
    public void should_reject_deploy_after_clear() throws Exception {
      subject.clear();
      error.expect(IllegalStateException.class);
      subject.deploy(StubContainer.create("test"));
    }

    @Test
    public void should_reject_drop_after_clear() throws Exception {
      subject.clear();
      error.expect(IllegalStateException.class);
      subject.drop(Schema.valueOf("test"));
    }

    @Test
    public void should_yield_remaining_on_clear() throws Exception {
      final Container container = StubContainer.create("test");
      subject.deploy(container);
      final Set<Container> remaining = subject.clear();
      assertThat(remaining, contains(container));
    }
  }


  public static class Eventing {
    private final CaptureEvents<CatalogEvent> events = CaptureEvents.create(CatalogEvent.class);
    private Catalog<Container> subject = new Catalog<>(events.bus(), TimeoutSpec.undefined());

    @Test
    public void emit_event_on_deploying_schema() throws Exception {
      subject.deploy(StubContainer.create("name"));
      assertThat(events.captured(), contains(instanceOf(CatalogEvent.SchemaDeployed.class)));
    }

    @Test
    public void emit_event_on_dropping_schema() throws Exception {
      subject.deploy(StubContainer.create("name"));
      subject.drop(Schema.valueOf("name"));
      assertThat(events.captured(), contains(instanceOf(CatalogEvent.SchemaDeployed.class), instanceOf(CatalogEvent.SchemaDropped.class)));
    }

    @Test
    public void emit_drop_and_deploy_when_replacing() throws Exception {
      subject.deploy(StubContainer.create("first"));
      subject.deploy(StubContainer.create("first"));
      assertThat(events.captured(), contains(instanceOf(CatalogEvent.SchemaDeployed.class), instanceOf(CatalogEvent.SchemaDropped.class), instanceOf(CatalogEvent.SchemaDeployed.class)));
    }

    @Test
    public void no_event_if_schema_not_present_on_drop() throws Exception {
      subject.drop(Schema.valueOf("name"));
      assertThat(events.captured(), is(emptyIterable()));
    }

    @Test
    public void emit_drop_for_all_remaining_on_close() throws Exception {
      subject.deploy(StubContainer.create("one"));
      subject.deploy(StubContainer.create("two"));
      subject.clear();
      assertThat(events.captured(), contains(
          instanceOf(CatalogEvent.SchemaDeployed.class), instanceOf(CatalogEvent.SchemaDeployed.class),
          instanceOf(CatalogEvent.SchemaDropped.class), instanceOf(CatalogEvent.SchemaDropped.class)
      ));
    }
  }


  public static class Locking {
    @Rule
    public final ExpectedException error = ExpectedException.none();

    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Catalog<Container> subject = new Catalog<>(new EventBus(), TimeoutSpec.undefined());

    @After
    public void shutdownExecutor() throws Exception {
      exec.shutdownNow();
    }

    @Test
    public void should_lock_stripe_for_given_name() throws Exception {
      subject.lock(Schema.valueOf("test"));
      assertThat(lockOf("test").isLocked(), equalTo(true));
    }

    @Test
    public void should_unlock_stripe_for_given_name() throws Exception {
      subject.lock(Schema.valueOf("test"));
      subject.unlock(Schema.valueOf("test"));
      assertThat(lockOf("test").isLocked(), equalTo(false));
    }

    @Test
    public void should_fail_if_stripe_locked() throws Exception {
      error.expect(UncheckedTimeoutException.class);
      exec.submit(new Runnable() {
        @Override
        public void run() {
          lockOf("test").lock();
        }
      }).get();
      subject.lock(Schema.valueOf("test"));
    }

    @Test
    public void should_return_result_of_atomic_callback() throws Exception {
      final Long result = subject.atomic(Schema.valueOf("test"), new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          return 1337L;
        }
      });
      assertThat(result, equalTo(1337L));
    }

    @Test
    public void should_execute_atomic_action_while_holding_lock() throws Exception {
      subject.atomic(Schema.valueOf("test"), new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          assertThat(lockOf("test").isHeldByCurrentThread(), equalTo(true));
          return null;
        }
      });
    }

    @Test
    public void should_release_lock_after_executing_the_callback() throws Exception {
      subject.atomic(Schema.valueOf("test"), new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          return null;
        }
      });
      assertThat(lockOf("test").isLocked(), equalTo(false));
    }

    @Test
    public void should_release_lock_even_if_callable_fails() throws Exception {
      error.expect(RuntimeException.class);
      try {
        subject.atomic(Schema.valueOf("test"), new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            throw new RuntimeException("error");
          }
        });
      } finally {
        assertThat(lockOf("test").isLocked(), equalTo(false));
      }
    }

    @Test
    public void should_fail_if_already_locked() throws Exception {
      error.expect(UncheckedTimeoutException.class);
      exec.submit(new Runnable() {
        @Override
        public void run() {
          lockOf("test").lock();
        }
      }).get();
      subject.atomic(Schema.valueOf("test"), new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          return null;
        }
      });
    }

    @Test
    public void should_rethrow_unchecked_exceptions_from_callback() throws Exception {
      final RuntimeException failure = new RuntimeException("test");
      error.expect(sameInstance(failure));
      subject.atomic(Schema.valueOf("test"), new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          throw failure;
        }
      });
    }

    @Test
    public void should_wrap_checked_exception_as_execution_exception() throws Exception {
      final Exception failure = new Exception("test");
      error.expect(UncheckedExecutionException.class);
      error.expectCause(sameInstance(failure));
      subject.atomic(Schema.valueOf("test"), new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          throw failure;
        }
      });
    }

    private ReentrantLock lockOf(final String name) {
      final Lock lock = subject.locks().get(Schema.valueOf(name));
      assert lock instanceof ReentrantLock : "unexpected lock type " + lock.getClass();
      return (ReentrantLock) lock;
    }
  }

  public static class Queries {

    private final Catalog<Container> subject =
        new Catalog<>(new EventBus(), TimeoutSpec.undefined());

    @Test
    public void yield_absent_if_no_container_with_id_deployed() throws Exception {
      final Optional<Container> found = subject.find(Schema.valueOf("test"));
      assertThat(found.isPresent(), equalTo(false));
    }

    @Test
    public void should_yield_deployed_container() throws Exception {
      final Container container = StubContainer.create("test");
      subject.deploy(container);
      final Optional<Container> found = subject.find(Schema.valueOf("test"));
      assertThat(found.get(), equalTo(container));
    }

    @Test
    public void found_containers_are_a_snapshot() throws Exception {
      final Collection<Container> containers =
          Arrays.<Container>asList(StubContainer.create("1"), StubContainer.create("2"));
      for (Container container : containers) {
        subject.deploy(container);
      }
      final Collection<Container> all = subject.findAll();
      assertThat(all, containsInAnyOrder(containers.toArray()));
      subject.drop(Schema.valueOf("1"));  // catalog changes, but snapshot should not
      assertThat(all, containsInAnyOrder(containers.toArray()));
    }

    @Test
    public void found_container_ids_are_a_snapshot() throws Exception {
      final Catalog<Container> subject = new Catalog<>(new EventBus(), TimeoutSpec.undefined());
      subject.deploy(StubContainer.create("1"));
      subject.deploy(StubContainer.create("2"));
      final Collection<Schema> keys = subject.findKeys();
      assertThat(keys, containsInAnyOrder(Schema.valueOf("1"), Schema.valueOf("2")));
      subject.drop(Schema.valueOf("1"));  // catalog changes, but snapshot should not
      assertThat(keys, containsInAnyOrder(Schema.valueOf("1"), Schema.valueOf("2")));
    }
  }
}
