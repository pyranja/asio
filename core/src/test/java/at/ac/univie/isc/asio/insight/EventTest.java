package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EventTest {

  @Test
  public void ensure_auto_value_argument_order_message_factory() throws Exception {
    final Message message = Message.create("test", ImmutableMap.of("key", "value"));
    assertThat(message.subject(), is("test"));
    assertThat(message.content(), is(Collections.singletonMap("key", "value")));
  }

  @Test
  public void ensure_auto_value_argument_order_on_event_factory() throws Exception {
    final Event event = Event.create(Correlation.valueOf("test"), 1L, Scope.SYSTEM, Message.create("test").empty());
    assertThat(event.correlation(), is(Correlation.valueOf("test")));
    assertThat(event.timestamp(), is(1L));
    assertThat(event.scope(), is(Scope.SYSTEM));
    assertThat(event.message(), is(Message.create("test").empty()));
  }
}
