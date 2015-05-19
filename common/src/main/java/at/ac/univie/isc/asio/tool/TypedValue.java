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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A string-convertible, immutable value type.
 * Implementations of this type should be transparent wrappers around a single value without
 * attached logic. The intention is to provide type safe primitive values, e.g. for identifiers.
 *
 * @param <VALUE> type of wrapped value
 */
@Immutable
public abstract class TypedValue<VALUE> {
  private final VALUE val;

  @JsonCreator
  protected TypedValue(@Nonnull final VALUE val) {
    requireNonNull(val, "id must not be null");
    this.val = normalize(val);
  }

  @JsonValue
  @Nonnull
  protected final VALUE value() {
    return val;
  }

  @Nonnull
  protected VALUE normalize(@Nonnull final VALUE val) {
    return val;
  }

  @Override
  public final String toString() {
    return val.toString();
  }

  @Override
  public final boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final TypedValue that = (TypedValue) other;

    return val.equals(that.val);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(val, this.getClass());
  }
}
