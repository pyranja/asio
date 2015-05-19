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
package at.ac.univie.isc.asio.platform;

import com.google.common.base.Ticker;

/**
 * {@code Ticker} that reads the system wall time with up to millisecond precision.
 *
 * @see System#currentTimeMillis()
 */
public final class CurrentTime extends Ticker {
  private static final CurrentTime INSTANCE = new CurrentTime();

  public static CurrentTime instance() {
    return INSTANCE;
  }

  private CurrentTime() {}

  @Override
  public long read() {
    return System.currentTimeMillis();
  }
}
