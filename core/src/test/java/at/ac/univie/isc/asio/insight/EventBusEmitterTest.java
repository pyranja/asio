package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.TestTicker;
import at.ac.univie.isc.asio.tool.CaptureEvents;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static at.ac.univie.isc.asio.tool.EventMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class EventBusEmitterTest {

  private final CaptureEvents<Event> events = CaptureEvents.create(Event.class);
  private final TestTicker time = TestTicker.create(0);

  private final EventBusEmitter subject = EventBusEmitter.create(events.bus(), time, Scope.SYSTEM);

  @Test
  public void should_send_emitted_message_to_event_bus() throws Exception {
    final Message message = Message.create("test", Collections.singletonMap("key", "value"));
    subject.emit(message);
    assertThat(events.single(), is(withMessage(message)));
  }

  @Test
  public void should_use_time_value_from_ticker_as_timestamp() throws Exception {
    final long expected = new Random().nextLong();
    time.setTime(expected);
    subject.emit(Message.empty("test"));
    assertThat(events.single().timestamp(), is(expected));
  }

  @Test
  public void should_use_same_correlation_for_different_messages() throws Exception {
    subject.emit(Message.create("first").empty());
    subject.emit(Message.create("second").empty());
    assertThat(events.captured(), correlated());
  }

  @Test
  public void should_use_different_correlations_in_different_instances() throws Exception {
    final EventBusEmitter first = EventBusEmitter.create(events.bus(), time, Scope.SYSTEM);
    final EventBusEmitter second = EventBusEmitter.create(events.bus(), time, Scope.SYSTEM);
    first.emit(Message.empty("test"));
    second.emit(Message.empty("test"));
    assertThat(events.captured(), not(correlated()));
  }

  @Test
  public void should_use_configured_scope() throws Exception {
    final EventBusEmitter emitter = EventBusEmitter.create(events.bus(), time, Scope.REQUEST);
    emitter.emit(Message.empty("test"));
    assertThat(events.single(), is(withScope("request")));
  }
}
