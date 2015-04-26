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
