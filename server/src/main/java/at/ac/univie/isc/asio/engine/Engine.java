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
 * An engine capable of creating invocations on a dataset for a specific
 * {@link Language}.
 */
public interface Engine extends AutoCloseable {

  /**
   * @return the {@link Language query language} supported by this engine.
   */
  Language language();

  /**
   * {@inheritDoc}
   */
  Invocation prepare(Command command);

  /**
   * Dispose used resources. Preparing new {@code Invocations} will not be possible afterwards and
   * currently running executions may fail.
   */
  @Override
  void close();
}
