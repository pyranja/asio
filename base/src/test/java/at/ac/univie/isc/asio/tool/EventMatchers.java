package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.insight.Event;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

public final class EventMatchers {
  private EventMatchers() { /* no instances  */ }

  @Factory
  public static EventTypeMatcher event(final String type) {
    return new EventTypeMatcher(type);
  }

  @Factory
  public static OrderedEventStreamMatcher orderedStreamOf(final Matcher<Event>... matchers) {
    return new OrderedEventStreamMatcher(Arrays.asList(matchers));
  }

  @Factory
  public static SingleCorrelationMatcher correlated() {
    return new SingleCorrelationMatcher();
  }

  static class EventTypeMatcher extends TypeSafeMatcher<Event> {
    private final String expected;

    EventTypeMatcher(final String expected) {
      this.expected = requireNonNull(expected);
    }

    @Override
    protected boolean matchesSafely(final Event item) {
      return item.message().equals(expected);
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("event of type [").appendValue(expected).appendText("]");
    }
  }

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
