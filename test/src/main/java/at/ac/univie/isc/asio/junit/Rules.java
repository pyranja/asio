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
package at.ac.univie.isc.asio.junit;

import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.primitives.Ints;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

public final class Rules {
  private Rules() {}

  /**
   * @param value max duration of test
   * @param unit unit of the max duration
   * @return A new {@link org.junit.rules.Timeout} rule with the given value.
   */
  public static Timeout timeout(final int value, final TimeUnit unit) {
    return new Timeout(Ints.checkedCast(unit.toMillis(value)));
  }

  /**
   * A light-weight, in-process http server.
   *
   * @param label describing this server
   * @return A new {@link at.ac.univie.isc.asio.web.HttpServer} rule
   */
  public static HttpServer httpServer(final String label) {
    return HttpServer.create(label);
  }

  /**
   * Enrich failure descriptions with reports from test collaborators.
   *
   * @return A new empty {@link Interactions} rule
   */
  public static Interactions interactions() {
    return Interactions.empty();
  }
}
