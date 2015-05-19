/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
