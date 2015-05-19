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
package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import com.hp.hpl.jena.rdf.model.Model;
import rx.Observable;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class StubContainer implements Container {

  public static StubContainer create(final String name) {
    return new StubContainer(Id.valueOf(name));
  }

  private final Id id;

  private boolean closed = false;

  private boolean activated = false;
  private Set<Engine> engines = new HashSet<>();
  private Observable<SchemaDescriptor> metadata = Observable.empty();
  private Observable<SqlSchema> definition = Observable.empty();
  private Observable<Model> mapping = Observable.empty();

  protected StubContainer(final Id id) {
    this.id = id;
  }

  @Override
  public Id name() {
    return id;
  }

  @Override
  public final Set<Engine> engines() {
    return engines;
  }

  @Override
  public Observable<SchemaDescriptor> metadata() {
    return metadata;
  }

  @Override
  public Observable<SqlSchema> definition() {
    return definition;
  }

  @Override
  public Observable<Model> mapping() {
    return mapping;
  }

  @Override
  public void activate() throws IllegalStateException {
    activated = true;
  }

  @Override
  public void close() {
    closed = true;
  }

  @Nonnull
  public StubContainer withEngine(final Engine engine) {
    engines.add(engine);
    return this;
  }

  @Nonnull
  public StubContainer withMetadata(final Observable<SchemaDescriptor> metadata) {
    this.metadata = metadata;
    return this;
  }

  @Nonnull
  public StubContainer withDefinition(final Observable<SqlSchema> definition) {
    this.definition = definition;
    return this;
  }

  @Nonnull
  public StubContainer withMapping(final Observable<Model> mapping) {
    this.mapping = mapping;
    return this;
  }

  // === test getter ===============================================================================

  public boolean isClosed() {
    return closed;
  }

  public boolean isActivated() {
    return activated;
  }

  public boolean isRunning() {
    return activated && !closed;
  }
}
