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

import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import rx.Observable;

import java.util.Map;
import java.util.Set;

/**
 * Wrap an {@code ApplicationContext}, which holds the components of a dataset.
 * This container <strong>must</strong> be {@link #close() closed} to release the spring context
 * and associated resources.
 */
@AutoValue
abstract class NestContainer implements Container, AutoCloseable {
  NestContainer() { /* prevent subclassing */ }

  public static NestContainer wrap(final ConfigurableApplicationContext context, final NestConfig config) {
    return new AutoValue_NestContainer(context, config, config.getDataset().getName());
  }

  /**
   * The wrapped spring context.
   */
  abstract ConfigurableApplicationContext context();

  // === bind container lifecycle to context lifecycle =============================================

  /**
   * Refresh the wrapped spring context. All component beans are created now.
   */
  @Override
  public void activate() {
    context().refresh();
  }

  /**
   * Close the wrapped spring context and release resources.
   */
  @Override
  public void close() {
    context().close();
  }

  // === container info ============================================================================

  @JsonProperty
  @JsonUnwrapped
  public abstract NestConfig configuration();

  // === component getters delegate to wrapped context =============================================

  /**
   * Name of the container, equal to the {@link ApplicationContext#getDisplayName()}.
   */
  @Override
  public abstract Id name();

  /**
   * Retrieve all beans that implement {@link Engine}.
   */
  @Override
  public final Set<Engine> engines() {
    final Map<String, Engine> found = context().getBeansOfType(Engine.class);
    return ImmutableSet.copyOf(found.values());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Observable<SqlSchema> definition() {
    return context().getBean(NestBluePrint.BEAN_DEFINITION_SOURCE, Observable.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Observable<SchemaDescriptor> metadata() {
    return context().getBean(NestBluePrint.BEAN_DESCRIPTOR_SOURCE, Observable.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Observable<Model> mapping() {
    return context().getBean(NestBluePrint.BEAN_MAPPING_SOURCE, Observable.class);
  }

  @Override
  public final String toString() {
    return "NestContainer{" + name() + " | " + configuration() + "}";
  }
}
