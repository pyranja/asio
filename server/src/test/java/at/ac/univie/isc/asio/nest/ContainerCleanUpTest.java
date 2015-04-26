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
