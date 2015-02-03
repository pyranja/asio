package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.insight.Event;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import org.hamcrest.*;

import java.util.Arrays;
import java.util.Queue;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.any;

public final class EventMatchers {
  private EventMatchers() { /* no instances  */ }

  @Deprecated
  @Factory
  public static EventContentMatcher event(final String type) {
    return new EventContentMatcher(any(String.class), equalTo(type));
  }

  @Factory
  public static EventContentMatcher event(final String scope, final String type) {
    return new EventContentMatcher(equalTo(scope), equalTo(type));
  }

  @Factory
  public static OrderedEventStreamMatcher orderedStreamOf(final Matcher<Event>... matchers) {
    return new OrderedEventStreamMatcher(Arrays.asList(matchers));
  }

  @Factory
  public static SingleCorrelationMatcher correlated() {
    return new SingleCorrelationMatcher();
  }

  /**
   * match event type and message
   */
  static class EventContentMatcher extends TypeSafeMatcher<Event> {
    private final Matcher<String> expectedScope;
    private final Matcher<String> expectedType;

    EventContentMatcher(final Matcher<String> expectedScope, final Matcher<String> expectedType) {
      this.expectedScope = expectedScope;
      this.expectedType = expectedType;
    }

    @Override
    protected boolean matchesSafely(final Event item) {
      return expectedScope.matches(item.type()) && expectedType.matches(item.message());
    }

    @Override
    public void describeTo(final Description description) {
      description
          .appendText("event of scope [").appendDescriptionOf(expectedScope).appendText("]")
          .appendText(" and of type [").appendDescriptionOf(expectedType).appendText("]");
    }
  }


  /**
   * match on all events sharing a correlation id
   */
  static class SingleCorrelationMatcher extends TypeSafeMatcher<Iterable<Event>> {
    @Override
    protected boolean matchesSafely(final Iterable<Event> item) {
      final Event first = Iterables.getFirst(item, null);
      if (first == null) { return false; }
      final Event.Correlation expected = first.correlation();
      for (Event event : item) {
        if (!expected.equals(event.correlation())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("an event stream sharing a single correlation id");
    }

    @Override
    protected void describeMismatchSafely(final Iterable<Event> item, final Description mismatchDescription) {
      mismatchDescription.appendText("found events ").appendValueList("[", ",", "]", item);
    }
  }


  /**
   * match against a sequence of expected events
   */
  static class OrderedEventStreamMatcher extends TypeSafeMatcher<Iterable<Event>> {
    private final Iterable<Matcher<Event>> expected;

    OrderedEventStreamMatcher(final Iterable<Matcher<Event>> expected) {
      this.expected = requireNonNull(expected);
    }

    @Override
    protected boolean matchesSafely(final Iterable<Event> item) {
      final Queue<Matcher<Event>> notYetMatched = Queues.newArrayDeque(expected);
      for (final Event actual : item) {
        if (notYetMatched.isEmpty()) {
          break;  // avoid NPE and stop early
        }
        if (notYetMatched.peek().matches(actual)) {
          notYetMatched.remove();
        }
      }
      return notYetMatched.isEmpty();
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("an event stream containing ")
          .appendValue(expected)
          .appendText(" in order");
    }

    @Override
    protected void describeMismatchSafely(final Iterable<Event> item, final Description mismatchDescription) {
      mismatchDescription.appendText("found events ").appendValueList("[", ",", "]", item);
    }
  }
}
