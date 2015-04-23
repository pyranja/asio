package at.ac.univie.isc.asio.insight;

import org.glassfish.jersey.media.sse.InboundEvent;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import rx.functions.Func1;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

/**
 * Hamcrest matchers for jersey server-sent-events.
 */
public final class Events {
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
   * Match if the event has an attribute with the given value.
   */
  public static Matcher<InboundEvent> attribute(final String key, final Matcher<? super Object> value) {
    final Matcher<? super Map<String, Object>> matcher = hasEntry(equalTo(key), value);
    return new EventWithAttribute(matcher);
  }

  /**
   * Match if the payload of the event can be converted to the given type and satisfies the expectation.
   */
  public static <PAYLOAD> Matcher<InboundEvent> payload(final Class<PAYLOAD> type,
                                                        final Matcher<? super PAYLOAD> expected) {
    return new EventWithPayload<>(type, expected);
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

  private Events() { /* static factory */ }
}
