package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Event;
import at.ac.univie.isc.asio.insight.Message;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import org.hamcrest.*;

import java.util.Arrays;
import java.util.Queue;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

public final class EventMatchers {
  private EventMatchers() { /* no instances  */ }

  @Factory
  public static Matcher<Event> event(final String scope, final String subject) {
    return Matchers.both(withScope(scope)).and(withSubject(subject));
  }

  @Factory
  public static Matcher<Event> withScope(final String scope) {
    return new EventScopeMatcher(equalToIgnoringCase(scope));
  }

  @Factory
  public static Matcher<Event> withSubject(final String subject) {
    return new EventSubjectMatcher(equalTo(subject));
  }

  @Factory
  public static Matcher<Event> withMessage(final Message message) {
    return new EventMessageMatcher(equalTo(message));
  }

  @Factory
  public static OrderedEventStreamMatcher orderedStreamOf(final Matcher<Event>... matchers) {
    return new OrderedEventStreamMatcher(Arrays.asList(matchers));
  }

  @Factory
  public static SingleCorrelationMatcher correlated() {
    return new SingleCorrelationMatcher();
  }

  static class EventMessageMatcher extends TypeSafeMatcher<Event> {
    private final Matcher<Message> expected;

    EventMessageMatcher(final Matcher<Message> expected) {
      this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(final Event item) {
      return expected.matches(item.message());
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("event with message ").appendDescriptionOf(expected);
    }
  }


  static class EventScopeMatcher extends TypeSafeMatcher<Event> {
    private final Matcher<String> expected;

    EventScopeMatcher(final Matcher<String> expected) {
      this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(final Event item) {
      return expected.matches(item.scope().toString());
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("event with scope ").appendDescriptionOf(expected);
    }
  }


  /**
   * match event scope and message
   */
  static class EventSubjectMatcher extends TypeSafeMatcher<Event> {
    private final Matcher<String> expectedSubject;

    EventSubjectMatcher(final Matcher<String> expectedSubject) {
      this.expectedSubject = expectedSubject;
    }

    @Override
    protected boolean matchesSafely(final Event item) {
      return expectedSubject.matches(item.message().subject());
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("event with subject ").appendDescriptionOf(expectedSubject);
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
      final Correlation expected = first.correlation();
      for (Event legacyEvent : item) {
        if (!expected.equals(legacyEvent.correlation())) {
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
