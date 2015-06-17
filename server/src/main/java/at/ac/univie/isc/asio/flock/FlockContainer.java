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
package at.ac.univie.isc.asio.flock;

import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import com.hp.hpl.jena.rdf.model.Model;
import rx.Observable;

import java.util.Collections;
import java.util.Set;

/**
 * A virtual dataset, supporting federated SPARQL queries.
 */
@AutoValue
abstract class FlockContainer implements Container {
  FlockContainer() { /* prevent subclassing */ }

  public static FlockContainer create(final FlockConfig config, final JenaEngine engine, final Observable<SchemaDescriptor> metadata) {
    final Set<Engine> engines = Collections.<Engine>singleton(engine);
    return new AutoValue_FlockContainer(config, engines, metadata);
  }

  @JsonProperty
  @JsonUnwrapped
  public abstract FlockConfig configuration();

  @Override
  public abstract Set<Engine> engines();

  @Override
  public abstract Observable<SchemaDescriptor> metadata();

  @Override
  public final Id name() {
    return configuration().getName();
  }

  // === noop lifecycle management =================================================================

  @Override
  public final void activate() throws IllegalStateException {}

  @Override
  public final void close() {}

  // === stub components ===========================================================================

  @Override
  public final Observable<SqlSchema> definition() {
    return Observable.empty();
  }

  @Override
  public final Observable<Model> mapping() {
    return Observable.empty();
  }
}
