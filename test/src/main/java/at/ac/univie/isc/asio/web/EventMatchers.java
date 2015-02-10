package at.ac.univie.isc.asio.web;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.Matchers.any;

public final class EventMatchers {
  private EventMatchers() {}

  /**
   * Creates a matcher for server-sent-events, where the event's type matches the given expectation.
   * @param expected matcher for the event type
   */
  @Factory
  public static Matcher<EventSource.MessageEvent> whereType(final Matcher<String> expected) {
    return new MessageEventMatcher(expected, any(String.class));
  }

  /**
   * Creates a matcher for server-sent-events, where the event's data matches the given expectation.
   * @param expected matcher for event data
   */
  @Factory
  public static Matcher<EventSource.MessageEvent> whereData(final Matcher<String> expected) {
    return new MessageEventMatcher(any(String.class), expected);
  }

  private static class MessageEventMatcher extends TypeSafeMatcher<EventSource.MessageEvent> {
    private final Matcher<String> expectedType;
    private final Matcher<String> expectedData;

    private MessageEventMatcher(final Matcher<String> expectedType, final Matcher<String> expectedData) {
      this.expectedType = expectedType;
      this.expectedData = expectedData;
    }

    @Override
    protected boolean matchesSafely(final EventSource.MessageEvent item) {
      return expectedType.matches(item.type()) && expectedData.matches(item.data());
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText(" event with type ").appendDescriptionOf(expectedType)
          .appendText(" and data ").appendDescriptionOf(expectedData);
    }
  }
}
