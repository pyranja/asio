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
package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.AsioError;
import at.ac.univie.isc.asio.tool.Pretty;

import java.net.URI;

/**
 * Thrown if the metadata repository cannot be reached or returned illegal responses.
 */
public final class RepositoryFailure extends AsioError.Base {
  public RepositoryFailure(final String message, final URI endpoint, final Throwable cause) {
    super(Pretty.format("failed to communicate with metadata repository at %s - %s", endpoint, message), cause);
  }
}
