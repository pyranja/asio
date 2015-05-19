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
package at.ac.univie.isc.asio.insight;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;

/**
 * Log {@link Event internal} and {@link com.google.common.eventbus.DeadEvent unhandled} events
 * to a {@link #LOGGER_NAME SLF4J logger}.
 */
@Component
public final class EventLoggerBridge {
  /** special logger name for events */
  public static final String LOGGER_NAME = "at.ac.univie.isc.asio.events";

  private static final Logger eventLog = LoggerFactory.getLogger(LOGGER_NAME);
  private static final Marker EVENT_MARKER = MarkerFactory.getMarker("EVENT");

  @Subscribe
  public void log(final Event event) {
    eventLog.info(EVENT_MARKER, "{}", event);
  }

  @Subscribe
  public void logDeadEvent(final DeadEvent dead) {
    eventLog.warn(EVENT_MARKER, "DEAD : {}", dead);
  }
}
