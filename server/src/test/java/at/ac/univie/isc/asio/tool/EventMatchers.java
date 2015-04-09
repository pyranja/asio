package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Event;
import com.google.common.collect.Iterables;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.CoreMatchers.equalTo;

public final class EventMatchers {
  private EventMatchers() { /* no instances  */ }

  public static Matcher<Event> event(final String subject) {
    return new EventSubjectMatcher(equalTo(subject));
  }

  public static SingleCorrelationMatcher correlated() {
    return new SingleCorrelationMatcher();
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
      return expectedSubject.matches(item.getSubject());
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
      final Correlation expected = first.getCorrelation();
      for (Event legacyEvent : item) {
        if (!expected.equals(legacyEvent.getCorrelation())) {
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


}
