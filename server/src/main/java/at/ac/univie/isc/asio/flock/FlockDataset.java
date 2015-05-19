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

import at.ac.univie.isc.asio.Dataset;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.metadata.DescriptorService;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import com.hp.hpl.jena.rdf.model.Model;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import rx.Observable;
import rx.functions.Func0;

import java.net.URI;
import java.util.Collections;

/**
 * Metadata on the virtual dataset in a flock server.
 */
final class FlockDataset implements Dataset {
  private static final ZonedDateTime LOAD_DATE = ZonedDateTime.now(ZoneOffset.UTC);

  /**
   * With generic, fixed metadata
   */
  public static FlockDataset withStaticMetadata(final URI identifier) {
    final SchemaDescriptor metadata = SchemaDescriptor.empty(identifier.toASCIIString())
        .withActive(true)
        .withLabel("flock")
        .withDescription("flock federated sparql processor")
        .withAuthor("n/a")
        .withCreated(LOAD_DATE)
        .withUpdated(LOAD_DATE)
        .withTags(Collections.singletonList("sparql"))
        .build();
    return new FlockDataset(Observable.just(metadata));
  }

  /**
   * Fetch metadata from the given remote service.
   */
  public static FlockDataset withDynamicMetadata(final URI identifier, final DescriptorService service) {
    return new FlockDataset(Observable.defer(new CallDescriptorService(service, identifier)));
  }

  private final Observable<SchemaDescriptor> metadata;

  private FlockDataset(final Observable<SchemaDescriptor> metadata) {
    this.metadata = metadata;
  }

  @Override
  public Id name() {
    return Id.valueOf("flock");
  }

  @Override
  public Observable<SchemaDescriptor> metadata() {
    return metadata;
  }

  @Override
  public Observable<SqlSchema> definition() {
    return Observable.empty();
  }

  @Override
  public Observable<Model> mapping() {
    return Observable.empty();
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
