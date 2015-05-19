/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio;

import com.google.common.base.Ticker;

/**
 * A fake {@code Ticker} for testing, where the time value can be set and modified. The value will
 * not change by itself.
 */
public class TestTicker extends Ticker {
  /**
   * @param initial time value
   * @return fake ticker starting with the given time value
   */
  public static TestTicker create(final long initial) {
    return new TestTicker(initial);
  }

  private long time;

  private TestTicker(final long initial) {
    this.time = initial;
  }

  @Override
  public long read() {
    return time;
  }

  /**
   * Set the time to a fixed value.
   * @param time new time value
   */
  public void setTime(final long time) {
    this.time = time;
  }

  /**
   * Increase the time value by the given amount.
   * @param by increase by this value
   */
  public void advance(final long by) {
    this.time += by;
  }
}
