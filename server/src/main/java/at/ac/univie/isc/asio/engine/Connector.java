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

import rx.Observable;

import javax.annotation.Nonnull;

/**
 * Invoke an engine operation and let clients observe the results.
 */
public interface Connector {
  /**
   * Attempt to invoke the operation specified by the given arguments. This method will not throw
   * exceptions, but will instead yield an error through the returned {@code Observable}.
   *
   * @param command requested operation
   * @return An observable sequence of {@code StreamedResults}
   */
  @Nonnull
  Observable<StreamedResults> accept(@Nonnull Command command);
}
