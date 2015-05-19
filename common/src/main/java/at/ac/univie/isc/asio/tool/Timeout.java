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
package at.ac.univie.isc.asio.tool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A type-safe, time-unit aware container for timeout configuration.
 */
@Immutable
public /* final */ class Timeout {
  private static final Timeout UNDEFINED = new Timeout(-1);

  /**
   * @return a timeout with no defined value
   */
  @Nonnull
  public static Timeout undefined() {
    return UNDEFINED;
  }

  /**
   * @param timeout literal value of the timeout
   * @param unit the time unit of the literal value, e.g. {@link java.util.concurrent.TimeUnit#SECONDS}
   * @return timeout with a defined value or {@link #undefined()} if given value is less than zero.
   */
  @Nonnull
  public static Timeout from(final long timeout, @Nonnull final TimeUnit unit) {
    if (timeout < 0) {
      return UNDEFINED;
    }
    return new Timeout(unit.toNanos(timeout));
  }

  private final long timeoutInNanos;
  private final boolean defined;

  private Timeout(final long timeout) {
    this.timeoutInNanos = timeout;
    this.defined = timeout >= 0;
  }

  /**
   * Get the timeout value converted to the given time-unit, or the fallback value, if the timeout
   * is not defined.
   * @param unit target time-unit
   * @param fallback default value
   * @return literal value of timeout in the target unit
   */
  public long getAs(@Nonnull final TimeUnit unit, final long fallback) {
    return defined ? unit.convert(timeoutInNanos, TimeUnit.NANOSECONDS) : fallback;
  }

  /**
   * @return {@code true} if this timeout has a defined value
   */
  public boolean isDefined() {
    return defined;
  }

  /**
   * @param fallback alternative timeout value
   * @return this if defined or the given alternative else
   */
  @Nonnull
  public Timeout orIfUndefined(@Nonnull final Timeout fallback) {
    requireNonNull(fallback, "cannot use null as fallback");
    return this.isDefined() ? this : fallback;
  }

  /* @VisibleForTesting */
  final long value() {
    return timeoutInNanos;
  }

  // === string serialization ======================================================================

  /**
   * Convert a text representation as created by {@link #toString()} into a timeout instance.
   *
   * @param text a string representing a timeout value
   * @return the parsed timeout instance
   * @throws IllegalArgumentException if the given string is not a representation of a timeout
   */
  @JsonCreator
  public static Timeout fromString(final String text) throws IllegalArgumentException {
    requireNonNull(text, "cannot parse <" + text + "> as timeout (null)");
    if ("undefined".equals(text)) {
      return Timeout.undefined();
    }
    if (text.endsWith("ms")) {
      final Long value = Long.valueOf(text.substring(0, text.length() - 2));
      return Timeout.from(value, TimeUnit.MILLISECONDS);
    }
    throw new IllegalArgumentException("cannot parse <" + text + "> as timeout (invalid format)");
  }

  /**
   * Convert this timeout into a {@code String}. The text format is either {@code undefined} if the
   * timeout is not defined or the timeout value in milliseconds and {@code ms} appended.
   * The text representation of this timeout can be round-tripped via {@link #fromString(String)}.
   *
   * @return text value of this timeout
   */
  @JsonValue
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (isDefined()) {
      sb.append(Long.toString(TimeUnit.NANOSECONDS.toMillis(timeoutInNanos))).append("ms");
    } else {
      sb.append("undefined");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    final Timeout that = (Timeout) o;
    return timeoutInNanos == that.timeoutInNanos;
  }

  @Override
  public int hashCode() {
    return (int) (timeoutInNanos ^ (timeoutInNanos >>> 32));
  }
}
