/*
 * #%L
 * asio integration
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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.hamcrest.*;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.Matchers.*;

/**
 * Hamcrest matchers for jersey server-sent-events.
 */
public final class Events {
  /**
   * Match if the name of the event satisfies the expectation.
   */
  public static Matcher<InboundEvent> name(final Matcher<? super String> expected) {
    return new EventWithName(expected);
  }

  /**
   * Match if the payload of the event is a text satisfying the expectation.
   */
  public static Matcher<InboundEvent> payload(final Matcher<? super String> expected) {
    return new EventWithText(expected);
  }

  /**
   * Match if the payload of the event can be converted to the given type and satisfies the expectation.
   */
  public static <PAYLOAD> Matcher<InboundEvent> payload(final Class<PAYLOAD> type,
                                                        final Matcher<? super PAYLOAD> expected) {
    return new EventWithPayload<>(type, expected);
  }

  // === json event matchers

  /**
   * Predicate for rxJava streams - filters on event type
   */
  public static Func1<InboundEvent, Boolean> only(final String... types) {
    final Matcher<InboundEvent> condition = attribute("type", Matchers.<Object>isOneOf(types));
    return new Func1<InboundEvent, Boolean>() {
      @Override
      public Boolean call(final InboundEvent inboundEvent) {
        return condition.matches(inboundEvent);
      }
    };
  }

  /**
   * Match if the event has an attribute with the given value.
   */
  public static Matcher<InboundEvent> attribute(final String key, final Matcher<? super Object> value) {
    final Matcher<? super Map<String, Object>> matcher = hasEntry(equalTo(key), value);
    return new EventWithAttribute(matcher);
  }

  /**
   * Match if a sequence of events shares a correlation id. Actual value of the correlation is
   * irrelevant. The matched sequence must contain at least one event.
   */
  public static Matcher<Iterable<? extends InboundEvent>> correlated() {
    return new CorrelatedEvents(null);
  }

  /**
   * Match if each element of a sequence of events has the given correlation.
   */
  public static Matcher<Iterable<? extends InboundEvent>> correlated(final String expected) {
    return new CorrelatedEvents(expected);
  }

  /**
   * Match if the event sequence contains events with the given subjects in order.
   */
  public static Matcher<Iterable<? extends InboundEvent>> sequence(final String... subjects) {
    final List<Matcher<? super InboundEvent>> subjectMatchers = new ArrayList<>();
    for (final String expected : subjects) {
      subjectMatchers.add(attribute("subject", Matchers.<Object>equalTo(expected)));
    }
    return Matchers.contains(subjectMatchers);
  }

  private static class EventWithName extends FeatureMatcher<InboundEvent, String> {
    public EventWithName(final Matcher<? super String> subMatcher) {
      super(subMatcher, "event with name", "name of event");
    }

    @Override
    protected String featureValueOf(final InboundEvent actual) {
      return actual.getName();
    }
  }


  private static class EventWithText extends FeatureMatcher<InboundEvent, String> {
    public EventWithText(final Matcher<? super String> subMatcher) {
      super(subMatcher, "event with text payload", "text of event payload");
    }

    @Override
    protected String featureValueOf(final InboundEvent actual) {
      return actual.readData();
    }
  }


  private static class EventWithAttribute extends FeatureMatcher<InboundEvent, Map<String, Object>> {
    public EventWithAttribute(final Matcher<? super Map<String, Object>> subMatcher) {
      super(subMatcher, "event with attributes", "attributes of event");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, Object> featureValueOf(final InboundEvent actual) {
      return actual.readData(Map.class);
    }
  }


  private static class EventWithPayload<PAYLOAD> extends FeatureMatcher<InboundEvent, PAYLOAD> {
    private final Class<PAYLOAD> type;

    public EventWithPayload(Class<PAYLOAD> type, final Matcher<? super PAYLOAD> subMatcher) {
      super(subMatcher, "event with payload", "payload of event");
      this.type = type;
    }

    @Override
    protected PAYLOAD featureValueOf(final InboundEvent actual) {
      return actual.readData(type);
    }
  }


  private static class CorrelatedEvents extends TypeSafeDiagnosingMatcher<Iterable<? extends InboundEvent>> {
    private final String fixedCorrelation;

    private CorrelatedEvents(final String fixedCorrelation) {
      this.fixedCorrelation = fixedCorrelation;
    }

    @Override
    protected boolean matchesSafely(final Iterable<? extends InboundEvent> item,
                                    final Description mismatch) {
      final Iterable<String> ids = Iterables.transform(item, new Function<InboundEvent, String>() {
        @Override
        public String apply(final InboundEvent input) {
          return Objects.toString(input.readData(Map.class).get("correlation"));
        }
      });
      final String expected = fixedCorrelation == null
          ? Iterables.getFirst(ids, null)
          : fixedCorrelation;
      final Matcher<Iterable<String>> subMatcher = everyItem(equalTo(expected));
      final boolean success = subMatcher.matches(ids);
      if (!success) {
        mismatch.appendText("correlations were ");
        subMatcher.describeMismatch(ids, mismatch);
      }
      return success;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("correlated event sequence");
      if (fixedCorrelation != null) {
        description.appendText(" sharing id ").appendValue(fixedCorrelation);
      }
    }
  }

  private Events() { /* static factory */ }
}
