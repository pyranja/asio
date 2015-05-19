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
package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.CaptureEvents;
import at.ac.univie.isc.asio.io.Payload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.testing.FakeTicker;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Random;

import static at.ac.univie.isc.asio.tool.EventMatchers.correlated;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class EventBusEmitterTest {
  private static class TestEvent extends Event {
    protected TestEvent() {
      super("test", Payload.randomText(20));
    }
  }

  private final CaptureEvents<Event> events = CaptureEvents.create(Event.class);
  private final FakeTicker time = new FakeTicker().advance(new Random().nextLong());
  private final Correlation correlation = Correlation.valueOf(Payload.randomText(30));

  private final EventBusEmitter subject = EventBusEmitter.create(events.bus(), time, correlation);

  @Test
  public void should_fallback_to_fixed_uuid_if_provider_fails() throws Exception {
    final EventBusEmitter subject = new EventBusEmitter(events.bus(), time, new Provider<Correlation>() {
          @Override
          public Correlation get() {
            throw new IllegalStateException("test");
          }
        });
    subject.emit(new TestEvent());
    assertThat(events.single().getCorrelation(), equalTo(EventBusEmitter.SYSTEM_CORRELATION));
  }
  
  @Test
  public void should_publish_event_to_bus() throws Exception {
    final Event event = new TestEvent();
    subject.emit(event);
    assertThat(events.single(), sameInstance(event));
  }

  @Test
  public void should_set_timestamp_from_ticker_value() throws Exception {
    subject.emit(new TestEvent());
    assertThat(events.single().getTimestamp(), equalTo(time.read()));
  }

  @Test
  public void should_set_fixed_correlation() throws Exception {
    subject.emit(new TestEvent());
    assertThat(events.single().getCorrelation(), equalTo(correlation));
  }

  @Test
  public void should_use_same_correlation_for_different_messages() throws Exception {
    subject.emit(new TestEvent());
    subject.emit(new TestEvent());
    assertThat(events.captured(), correlated());
  }

  @Test
  public void should_use_different_correlations_in_different_instances() throws Exception {
    final EventBusEmitter first = EventBusEmitter.create(events.bus(), time, Correlation.valueOf("one"));
    final EventBusEmitter second = EventBusEmitter.create(events.bus(), time, Correlation.valueOf("two"));
    first.emit(new TestEvent());
    second.emit(new TestEvent());
    assertThat(events.captured(), not(correlated()));
  }

  @Test
  public void should_emit_error_event_for_exceptions() throws Exception {
    subject.emit(new IllegalArgumentException("test"));
    assertThat(events.single().getType(), equalTo("error"));
    assertThat(events.single().getSubject(), equalTo("error"));
  }

  @Test
  public void should_emit_error_event_with_set_correlation() throws Exception {
    subject.emit(new IllegalArgumentException("test"));
    assertThat(events.single().getCorrelation(), equalTo(correlation));
  }

  @Test
  public void should_emit_error_event_with_timestamp_from_ticker() throws Exception {
    subject.emit(new IllegalArgumentException("test"));
    assertThat(events.single().getTimestamp(), equalTo(time.read()));
  }

  @Test
  public void should_not_duplicate_timestamp_in_error_wrapper() throws Exception {
    subject.emit(new IllegalArgumentException("test"));
    final String json = new ObjectMapper().writeValueAsString(events.single());
    final int first = json.indexOf("\"timestamp\":");
    assertThat("duplicate timestamp in " + json, json.indexOf("\"timestamp\":", first + 1), lessThan(0));
  }
}
