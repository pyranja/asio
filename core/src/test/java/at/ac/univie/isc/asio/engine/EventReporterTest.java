package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.admin.Event;
import at.ac.univie.isc.asio.tool.CaptureEvents;
import at.ac.univie.isc.asio.tool.TestTicker;
import org.junit.Test;

import java.util.Arrays;

import static at.ac.univie.isc.asio.tool.EventMatchers.correlated;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class EventReporterTest {
  public static final String TEST_MESSAGE = "test";

  private final CaptureEvents events = CaptureEvents.create();
  private final TestTicker time = TestTicker.create(0);
  private EventReporter subject = new EventReporter(events.bus(), time);

  @Test
  public void emits_created_event() throws Exception {
    final Event event = subject.event(TEST_MESSAGE);
    assertThat(events.captured(Event.class), contains(event));
  }

  @Test
  public void uses_same_id_as_correlation() throws Exception {
    final Event first = subject.event(TEST_MESSAGE);
    final Event second = subject.event(TEST_MESSAGE);
    assertThat(Arrays.asList(first, second), is(correlated()));
  }

  @Test
  public void different_instances_use_distinct_correlations() throws Exception {
    final Event first = new EventReporter(events.bus(), TestTicker.create(0)).event(TEST_MESSAGE);
    final Event second = new EventReporter(events.bus(), TestTicker.create(0)).event(TEST_MESSAGE);
    assertThat(Arrays.asList(first, second), is(not(correlated())));
  }

  @Test
  public void uses_ticker_value_as_timestamp() throws Exception {
    time.setTime(42L);
    final Event event = subject.event(TEST_MESSAGE);
    assertThat(event.timestamp(), is(42L));
  }

  @Test
  public void emits_request_type_events() throws Exception {
    final Event event = subject.event(TEST_MESSAGE);
    assertThat(event.type(), is(EventReporter.REQUEST_TYPE.toString()));
  }
}
