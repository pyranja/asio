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
package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.database.Jdbc;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@AutoValue
abstract class NestConfig {
  /**
   * Create config holder with default settings only.
   */
  static NestConfig empty() {
    return create(new Dataset(), new Jdbc(), D2rqConfigModel.wrap(ModelFactory.createDefaultModel()));
  }

  /**
   * Create holder with given config beans.
   */
  static NestConfig create(final Dataset dataset, final Jdbc jdbc, final D2rqConfigModel d2rq) {
    return new AutoValue_NestConfig(dataset, jdbc, d2rq);
  }

  /**
   * General settings of the dataset.
   */
  @JsonUnwrapped
  public abstract Dataset getDataset();

  /**
   * Describe the connection to the backing database.
   */
  public abstract Jdbc getJdbc();

  /**
   * The d2rq configuration.
   */
  @JsonIgnore
  public abstract D2rqConfigModel getD2rq();
}
