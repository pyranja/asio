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

import at.ac.univie.isc.asio.Flock;
import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.FixedSelection;
import at.ac.univie.isc.asio.engine.sparql.DefaultJenaFactory;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sparql.JenaFactory;
import at.ac.univie.isc.asio.insight.EventResource;
import at.ac.univie.isc.asio.metadata.DescriptorService;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.tool.Timeout;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Set;

/**
 * Configure the single dataset of a flock server.
 */
@Configuration
@Flock
@EnableConfigurationProperties(FlockSettings.class)
class FlockComponents {

  @Autowired
  private FlockSettings config;

  @Autowired(required = false)
  private DescriptorService descriptorService;

  @Bean
  public FlockDataset flockDataset() {
    if (descriptorService == null) {
      return FlockDataset.withStaticMetadata(config.getIdentifier());
    } else {
      return FlockDataset.withDynamicMetadata(config.getIdentifier(), descriptorService);
    }
  }

  @Bean
  public FixedSelection fixedEngineRouter(final Set<Engine> engines) {
    return FixedSelection.from(engines);
  }

  @Bean
  public JenaEngine jenaEngine(final JenaFactory factory) {
    return JenaEngine.using(factory, true);
  }

  @Bean
  public JenaFactory simpleJenaState(final Timeout timeout) {
    return new DefaultJenaFactory(ModelFactory.createDefaultModel(), timeout);
  }

  @Bean
  @Primary
  public ResourceConfig flockJerseyConfiguration(final ResourceConfig config) {
    config.setApplicationName("jersey-flock");
    config.register(DatasetResource.class);
    config.register(EventResource.class);
    config.register(WhoamiResource.class);
    return config;
  }
}
