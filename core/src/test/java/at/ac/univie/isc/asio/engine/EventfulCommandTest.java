package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.admin.Event;
import at.ac.univie.isc.asio.tool.CaptureEvents;
import com.google.common.base.Ticker;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import rx.Observable;

import java.io.OutputStream;

import static at.ac.univie.isc.asio.tool.EventMatchers.*;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class EventfulCommandTest {
  private final CaptureEvents events = CaptureEvents.create();
  private final Command wrapped = mock(Command.class);
  private final Command.Results results = mock(Command.Results.class);
  private final EventReporter builder = new EventReporter(events.bus(), Ticker.systemTicker());
  private final EventfulCommandDecorator.EventfulCommand subject =
      new EventfulCommandDecorator.EventfulCommand(wrapped, builder);

  @Test
  public void successful_execution() throws Exception {
    when(wrapped.observe()).thenReturn(Observable.just(results));
    subject.observe().toBlocking().single().write(ByteStreams.nullOutputStream());
    assertThat(events.captured(Event.class),
        is(both(orderedStreamOf(event("executed"), event("completed"))).and(correlated())));
  }

  @Test
  public void execution_fails() throws Exception {
    when(wrapped.observe()).thenReturn(Observable.<Command.Results>error(new IllegalStateException("test")));
    try {
      subject.observe().toBlocking().single();
    } catch (IllegalStateException ignored) {}
    assertThat(events.captured(Event.class),
        is(both(orderedStreamOf(event("failed"))).and(correlated())));
  }

  @Test
  public void writing_fails() throws Exception {
    doThrow(new IllegalStateException("test")).when(results).write(any(OutputStream.class));
    when(wrapped.observe()).thenReturn(Observable.just(results));
    try {
      subject.observe().toBlocking().single().write(ByteStreams.nullOutputStream());
    } catch (IllegalStateException ignored) {}
    assertThat(events.captured(Event.class),
        is(both(orderedStreamOf(event("executed"), event("failed"))).and(correlated())));
  }
}
