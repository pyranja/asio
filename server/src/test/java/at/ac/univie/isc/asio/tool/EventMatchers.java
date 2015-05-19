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
package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Event;
import com.google.common.collect.Iterables;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.CoreMatchers.equalTo;

public final class EventMatchers {
  private EventMatchers() { /* no instances  */ }

  public static Matcher<Event> hasSubject(final String expected) {
    return new EventSubjectMatcher(equalTo(expected));
  }

  public static Matcher<Event> hasType(final String expected) {
    return new EventTypeMatcher(equalTo(expected));
  }

  public static SingleCorrelationMatcher correlated() {
    return new SingleCorrelationMatcher();
  }

  /** match subject of event */
  static final class EventSubjectMatcher extends FeatureMatcher<Event, String> {
    EventSubjectMatcher(final Matcher<String> expectedSubject) {
      super(expectedSubject, "event with subject", "subject of event");
    }

    @Override
    protected String featureValueOf(final Event actual) {
      return actual.getSubject();
    }
  }

  /** match type of event */
  static final class EventTypeMatcher extends FeatureMatcher<Event, String> {
    private EventTypeMatcher(final Matcher<? super String> subMatcher) {
      super(subMatcher, "event with type", "type of event");
    }
    @Override
    protected String featureValueOf(final Event actual) {
      return actual.getType();
    }
  }

  /**
   * match on all events sharing a correlation id
   */
  static final class SingleCorrelationMatcher extends TypeSafeMatcher<Iterable<Event>> {
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
