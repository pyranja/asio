package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.jaxrs.AsyncResponseFake;
import at.ac.univie.isc.asio.tool.Reactive;
import org.hamcrest.Matchers;
import org.junit.Test;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AsyncListenerTest {
  private final Subscription subscription = Subscriptions.create(Reactive.noop());
  private final AsyncListener subject = AsyncListener.cleanUp(subscription);

  @Test
  public void should_implement_callback_interfaces() throws Exception {
    assertThat(subject, instanceOf(TimeoutHandler.class));
    assertThat(subject, instanceOf(ConnectionCallback.class));
    assertThat(subject, instanceOf(CompletionCallback.class));
  }

  @Test
  public void should_register_as_callback() throws Exception {
    final AsyncResponseFake async = AsyncResponseFake.create();
    subject.listenTo(async);
    assertThat(async.callbacks(), Matchers.<Object>contains(subject));
  }

  @Test
  public void should_register_as_timeout_handler() throws Exception {
    final AsyncResponseFake async = AsyncResponseFake.create();
    subject.listenTo(async);
    assertThat(async.timeoutHandler(), Matchers.<TimeoutHandler>is(subject));
  }

  @Test
  public void should_resume_response_with_error_on_timeout() throws Exception {
    final AsyncResponseFake async = AsyncResponseFake.create();
    subject.handleTimeout(async);
    assertThat(async.error(), instanceOf(ServiceUnavailableException.class));
  }

  @Test
  public void should_unsubscribe_on_timeout() throws Exception {
    subject.handleTimeout(AsyncResponseFake.create());
    assertThat(subscription.isUnsubscribed(), is(true));
  }

  @Test
  public void should_unsubscribe_on_disconnect() throws Exception {
    subject.onDisconnect(AsyncResponseFake.create());
    assertThat(subscription.isUnsubscribed(), is(true));
  }

  @Test
  public void should_unsubscribe_on_successful_completion() throws Exception {
    subject.onComplete(null);
    assertThat(subscription.isUnsubscribed(), is(true));
  }

  @Test
  public void should_unsubscribe_on_failed_completion() throws Exception {
    subject.onComplete(new IllegalStateException("test"));
    assertThat(subscription.isUnsubscribed(), is(true));
  }
}
