/*
 * #%L
 * asio common
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Base for event objects. Define generic event properties, concrete events should add contextual
 * properties.
 */
public abstract class Event {
  /**
   * Create an event holding only the {@link #RESERVED_PROPERTIES minimal set} of attributes.
   */
  public static Event simple(final String type, final String subject) {
    return new SimpleEvent(type, subject);
  }

  private static class SimpleEvent extends Event {
    private SimpleEvent(final String type, final String subject) {
      super(type, subject);
    }
  }

  /**
   * Names of the required event properties. These may not be overwritten.
   */
  static final Set<String> RESERVED_PROPERTIES =
      ImmutableSet.of("type", "subject", "correlation", "timestamp");

  /**
   * Predicate to test if a property may be included in an event object.
   */
  static final Predicate<String> VALID_PROPERTY_NAME = new Predicate<String>() {
    @Override
    public boolean apply(final String input) {
      return !RESERVED_PROPERTIES.contains(input);
    }
  };

  private final String type;
  private final String subject;

  private Correlation correlation = null;
  private long timestamp = 0;

  @JsonCreator
  protected Event(@JsonProperty("type") final String type,
                  @JsonProperty("subject") final String subject) {
    this.type = type;
    this.subject = subject;
  }

  /**
   * Inject the correlation and timestamp into this event.
   * An event must be initialized exactly once.
   *
   * @throws AssertionError if the event is initialized more than once
   */
  final Event init(final Correlation correlation, final long timestamp) {
    assert this.correlation == null : "already initialized";
    this.correlation = requireNonNull(correlation);
    this.timestamp = timestamp;
    return this;
  }

  /**
   * A reference to identify correlated events, e.g. all events from a single request will have the
   * same correlation value.
   */
  public final Correlation getCorrelation() {
    return correlation;
  }

  /**
   * The milliseconds since epoch, when this event was recorded.
   */
  public final long getTimestamp() {
    return timestamp;
  }

  /**
   * jackson setter
   */
  private void setCorrelation(final Correlation correlation) {
    this.correlation = correlation;
  }

  /**
   * jackson setter
   */
  private void setTimestamp(final long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * The category of this event.
   */
  public final String getType() {
    return type;
  }

  /**
   * A keyword describing the type of event, e.g. 'failed' (with a scope of request) to indicate a
   * terminal failure during a request.
   */
  public final String getSubject() {
    return subject;
  }

  @Override
  public final String toString() {
    return "{" +
        "type='" + type + '\'' +
        ", subject='" + subject + '\'' +
        ", correlation=" + correlation +
        ", timestamp=" + timestamp +
        '}';
  }
}
