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
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;

/**
 * Route {@link Command commands} to matching
 * {@link at.ac.univie.isc.asio.engine.Engine handlers}.
 */
public interface EngineRouter {
  /**
   * Select an appropriate handler for the given command.
   * @param command describes the request
   * @return An {@code Engine} capable of handling the command
   * @throws Language.NotSupported if no matching {@code Engine} is found
   */
  Engine select(Command command) throws Language.NotSupported;
}
