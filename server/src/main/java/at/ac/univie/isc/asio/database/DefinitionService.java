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
package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.SqlSchema;
import rx.Observable;

/**
 * Define a service to inspect structural information on a dataset in the backing database.
 */
public interface DefinitionService {
  /**
   * Inspect the backing database schema of a dataset. Results include structural information on
   * relational tables, columns and relationships. The {@code Observable} may either return a single
   * definition on success or an error if the inspection failed, e.g. the target schema does not
   * exist.
   *
   * @param name name of the schema in the database
   * @return reactive sequence of inspection results
   */
  Observable<SqlSchema> definition(final String name);
}
