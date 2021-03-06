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

/**
 * Enrich simple events and emit them through the event system.
 */
public interface Emitter {

  /**
   * Emit the given, context-free event. The event <strong>must not</strong> be
   * {@link Event#init(Correlation, long) initialized} yet.
   *
   * @param event an uninitialized event
   * @return the initialized event as it has been published
   */
  Event emit(Event event);

  /**
   * Emit an event describing the given exception.
   *
   * @param exception the exception that occurred
   * @return the error event as it has been published
   */
  VndError emit(Throwable exception);
}
