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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

/**
 * A temporal duration, measured as a long value in a specific {@link java.util.concurrent.TimeUnit}.
 */
@Immutable
public final class Duration extends Pair<Long, TimeUnit> {
  public static Duration create(@Nonnull final Long length, @Nonnull TimeUnit unit) {
    return new Duration(length, unit);
  }

  private Duration(final Long length, final TimeUnit unit) {
    super(length, unit);
  }

  /**
   * @return length of this duration
   */
  public long length() {
    return first();
  }

  /**
   * @return unit of this duration
   */
  @Nonnull
  public TimeUnit unit() {
    return second();
  }
}
