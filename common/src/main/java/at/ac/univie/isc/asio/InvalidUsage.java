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

import javax.annotation.Nonnull;

/**
 * A base class for exceptions, which represent failures due to a client error,
 * e.g. a request with illegal set of parameters.
 */
public abstract class InvalidUsage extends AsioError.Base {

  /**
   * A descriptive label of this error. Guaranteed to be non-null and non-empty.
   */
  @Nonnull
  public final String getLabel() {
    final String message = getMessage();
    return (message == null || message.isEmpty()) ? getClass().getSimpleName() : message;
  }

  // === subclass constructors =====================================================================

  /** Construct with given message */
  protected InvalidUsage(final String message) {
    super(message);
  }

  /** construct with message and cause */
  protected InvalidUsage(final String message, final Throwable cause) {
    super(message, cause);
  }
}
