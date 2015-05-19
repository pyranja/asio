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

import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class Pair<FIRST, SECOND> {

  private final FIRST first;
  private final SECOND second;

  protected Pair(@Nonnull final FIRST first, @Nonnull final SECOND second) {
    this.first = requireNonNull(first);
    this.second = requireNonNull(second);
  }

  @Nonnull
  protected final FIRST first() {
    return first;
  }

  @Nonnull
  protected final SECOND second() {
    return second;
  }

  @Override
  public final String toString() {
    return Objects.toStringHelper(this)
        .addValue(first)
        .addValue(second)
        .toString();
  }

  @Override
  public final boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final Pair pair = (Pair) other;

    if (first != null ? !first.equals(pair.first) : pair.first != null) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (second != null ? !second.equals(pair.second) : pair.second != null) {
      return false;
    }

    return true;
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(first, second, this.getClass());
  }
}
