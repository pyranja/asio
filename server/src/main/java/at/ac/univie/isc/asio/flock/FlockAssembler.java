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
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.brood.Assembler;
import at.ac.univie.isc.asio.engine.sparql.DefaultJenaFactory;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.metadata.DescriptorService;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.tool.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import rx.Observable;
import rx.functions.Func0;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Factory for flock containers.
 */
@Component
public /* final */ class FlockAssembler implements Assembler {
  private static final Logger log = getLogger(FlockAssembler.class);

  private static final ZonedDateTime LOAD_DATE = ZonedDateTime.now(ZoneOffset.UTC);

  private final ObjectMapper jackson;
  private final Timeout globalTimeout;
  // static metadata as default fallback
  private DescriptorService descriptorService = new DescriptorService() {
    @Override
    public Observable<SchemaDescriptor> metadata(final URI identifier) {
      return Observable.just(SchemaDescriptor.empty(identifier.toASCIIString())
              .withActive(true)
              .withLabel("flock")
              .withDescription("flock federated sparql processor")
              .withAuthor("n/a")
              .withCreated(LOAD_DATE)
              .withUpdated(LOAD_DATE)
              .withTags(Collections.singletonList("sparql"))
              .build()
      );
    }
  };

  @Autowired
  FlockAssembler(final ObjectMapper jackson, final Timeout globalTimeout) {
    this.jackson = jackson;
    this.globalTimeout = globalTimeout;
  }

  @Autowired(required = false)
  public FlockAssembler setDescriptorService(final DescriptorService descriptorService) {
    this.descriptorService = descriptorService;
    return this;
  }

  @Override
  public Container assemble(final Id name, final ByteSource source) {
    final FlockConfig config = parse(source);
    log.debug(Scope.SYSTEM.marker(), "parsed virtual dataset config as {}", config);

    final Timeout timeout = config.getTimeout().orIfUndefined(globalTimeout);
    config.setName(name).setTimeout(timeout);
    log.info(Scope.SYSTEM.marker(), "assembling virtual dataset from {}", config);

    final DefaultJenaFactory jenaFactory =
        new DefaultJenaFactory(ModelFactory.createDefaultModel(), timeout);
    final JenaEngine engine = JenaEngine.using(jenaFactory, true);

    final Observable<SchemaDescriptor> descriptor =
        Observable.defer(new CallDescriptorService(descriptorService, config.getIdentifier()));

    return FlockContainer.create(config, engine, descriptor);
  }

  private FlockConfig parse(final ByteSource source) {
    try {
      return jackson.readValue(source.read(), FlockConfig.class);
    } catch (IOException e) {
      throw new InvalidFlockConfiguration(e);
    }
  }

  private static class CallDescriptorService implements Func0<Observable<? extends SchemaDescriptor>> {
    private final DescriptorService service;
    private final URI identifier;

    public CallDescriptorService(final DescriptorService serviceRef, final URI identifierRef) {
      this.service = serviceRef;
      this.identifier = identifierRef;
    }

    @Override
    public Observable<? extends SchemaDescriptor> call() {
      return service.metadata(identifier);
    }
  }
}
