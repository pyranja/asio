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

import rx.Observable;

import java.net.URI;

/**
 * Define a service to retrieve metadata of datasets from some (external) source. Metadata lookup
 * requires a global identifier of the requesting dataset.
 */
public interface DescriptorService {
  /**
   * Request metadata for the dataset with the given {@code identifier}. The {@code Observable} may
   * either return a single descriptor on success, nothing at all if no metadata is found, or raise
   * an error if retrieval fails.
   *
   * @param identifier global identifier of the requesting dataset
   * @return reactive sequence of lookup results
   */
  Observable<SchemaDescriptor> metadata(final URI identifier);
}
