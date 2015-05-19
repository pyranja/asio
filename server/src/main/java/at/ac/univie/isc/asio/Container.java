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
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.Engine;

import java.util.Set;

/**
 * Facade to a single data set.
 */
public interface Container extends AutoCloseable, Dataset {

  /**
   * All configured engines for this schema, i.e. sql and sparql.
   *
   * @return set of sql and sparql engine
   */
  Set<Engine> engines();

  // === lifecycle =================================================================================

  /**
   * Allocate required resources and attempt to start all components, which are part of this
   * container. A container may only be activated once.
   *
   * @throws IllegalStateException if activated more than once.
   */
  void activate() throws IllegalStateException;

  /**
   * Release all resources associated to this container.
   */
  @Override
  void close();
}
