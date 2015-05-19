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
package at.ac.univie.isc.asio;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Locale;

/**
 * A classification dimension, referring to the type and extent of the classified activity or event.
 */
public enum Scope {
  /** spans the whole asio instance, e.g. configuration changes, startup/shutdown. */
  SYSTEM,
  /** pertains to a single request from a client, e.g. a query execution */
  REQUEST;

  private final Marker marker;

  private Scope() {
    marker = MarkerFactory.getMarker(name());
  }

  /**
   * The {@link org.slf4j.Marker} associated with this scope for logging purposes.
   * @return the marker representing this scope
   */
  public final Marker marker() {
    return marker;
  }

  @Override
  public final String toString() {
    return name().toLowerCase(Locale.ENGLISH);
  }
}
