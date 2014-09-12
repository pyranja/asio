package at.ac.univie.isc.asio.tool;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Arrays;

import static at.ac.univie.isc.asio.tool.Reactive.listeningFor;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ToObservableListenableFutureTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private TestSubscriber<Object> subscriber = new TestSubscriber<>();
  private SettableFuture<Object> future = SettableFuture.create();

  @Before
  public void setup() {
    Observable.create(listeningFor(future)).unsafeSubscribe(subscriber);
  }

  @Test
  public void should_yield_future_result() throws Exception {
    final Object it = new Object();
    future.set(it);
    subscriber.assertReceivedOnNext(Arrays.asList(it));
    subscriber.assertTerminalEvent();
  }

  @Test
  public void should_reject_null_future() throws Exception {
    error.expect(NullPointerException.class);
    listeningFor(null);
  }

  @Test
  public void should_yield_cause_if_future_fails() throws Exception {
    final RuntimeException failure = new RuntimeException("test");
    future.setException(failure);
    assertThat(subscriber.getOnErrorEvents(), is(equalTo(Arrays.<Throwable>asList(failure))));
    subscriber.assertTerminalEvent();
  }

  @Test
  public void should_cancel_future_when_unsubscribing() throws Exception {
    subscriber.unsubscribe();
    assertThat(future.isCancelled(), is(true));
  }
}
