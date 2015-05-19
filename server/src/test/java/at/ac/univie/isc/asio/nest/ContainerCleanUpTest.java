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
package at.ac.univie.isc.asio.nest;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.StaticApplicationContext;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ContainerCleanUpTest {
  private final StaticApplicationContext context = new StaticApplicationContext();
  private final NestConfig config = NestConfig.empty();
  private final OnClose action = Mockito.mock(OnClose.class);

  private final D2rqNestAssembler.ContainerCleanUp subject =
      new D2rqNestAssembler.ContainerCleanUp(context, config, Collections.singletonList(action));

  @Test
  public void should_invoke_actions_on_context_closed() throws Exception {
    subject.onApplicationEvent(new ContextClosedEvent(context));
    verify(action).cleanUp(config);
  }

  @Test
  public void should_ignore_close_events_of_other_contexts() throws Exception {
    subject.onApplicationEvent(new ContextClosedEvent(new StaticApplicationContext()));
    verifyZeroInteractions(action);
  }

  @Test
  public void should_be_triggered_from_context_close() throws Exception {
    context.getBeanFactory().registerSingleton("clean-up", subject);
    context.refresh();
    context.close();
    verify(action).cleanUp(config);
  }
}
