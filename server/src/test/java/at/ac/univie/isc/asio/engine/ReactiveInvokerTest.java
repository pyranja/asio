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

import at.ac.univie.isc.asio.security.Authorizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import javax.ws.rs.ForbiddenException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ReactiveInvokerTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Invocation invocation = Mockito.mock(Invocation.class);
  private final Engine engine = Mockito.mock(Engine.class);
  private final EngineRouter router = Mockito.mock(EngineRouter.class);
  private final Authorizer authorizer = Mockito.mock(Authorizer.class);

  private final ReactiveInvoker
      subject = ReactiveInvoker.from(router, Schedulers.newThread(), authorizer);

  private final TestSubscriber<StreamedResults> subscriber = new TestSubscriber<>();

  @Before
  public void setUp() throws Exception {
    RequestContextHolder.setRequestAttributes(Mockito.mock(RequestAttributes.class));
    when(router.select(any(Command.class))).thenReturn(engine);
    when(engine.prepare(any(Command.class))).thenReturn(invocation);
  }

  @After
  public void tearDown() throws Exception {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  public void should_execute_invocation_from_selected_engine() throws Exception {
    subject.accept(CommandBuilder.empty().build()).toBlocking().single();
    verify(invocation).execute();
  }

  @Test
  public void should_subscribe_on_scheduler() throws Exception {
    subject.accept(CommandBuilder.empty().build()).subscribe(subscriber);
    subscriber.awaitTerminalEvent(2, TimeUnit.SECONDS);
    assertThat(subscriber.getLastSeenThread(), is(not(Thread.currentThread())));
  }

  @Test
  public void should_execute_invocation_in_captured_context() throws Exception {
    final Boolean contextPresent = subject.accept(CommandBuilder.empty().build())
        .isEmpty()
        .map(new Func1<Boolean, Boolean>() {
          @Override
          public Boolean call(final Boolean aBoolean) {
            return RequestContextHolder.getRequestAttributes() != null;
          }
        }).toBlocking()
        .single();
    assertThat(contextPresent, equalTo(true));
  }

  @Test
  public void should_escalate_invocation_error() throws Exception {
    final IllegalStateException cause = new IllegalStateException();
    when(engine.prepare(any(Command.class))).thenThrow(cause);
    error.expect(is(cause));
    subject.accept(CommandBuilder.empty().build()).toBlocking().single();
  }

  @Test
  public void should_escalate_authorization_failure() throws Exception {
    doThrow(ForbiddenException.class).when(authorizer).check(any(Invocation.class));
    error.expect(ForbiddenException.class);
    subject.accept(CommandBuilder.empty().build()).toBlocking().single();
  }

  @Test
  public void should_escalate_failure_if_command_is_invalid() throws Exception {
    error.expect(IllegalArgumentException.class);
    error.expectMessage("test");
    subject.accept(Command.invalid(new IllegalArgumentException("test"))).toBlocking().single();
  }

  @Test
  public void should_escalate_selection_error() throws Exception {
    final IllegalStateException failure = new IllegalStateException("test");
    when(router.select(any(Command.class))).thenThrow(failure);
    error.expect(is(failure));
    subject.accept(CommandBuilder.empty().build()).toBlocking().single();
  }

  @Test
  public void should_escalate_engine_error() throws Exception {
    final IllegalStateException failure = new IllegalStateException("test");
    when(engine.prepare(any(Command.class))).thenThrow(failure);
    error.expect(is(failure));
    subject.accept(CommandBuilder.empty().build()).toBlocking().single();
  }
}
