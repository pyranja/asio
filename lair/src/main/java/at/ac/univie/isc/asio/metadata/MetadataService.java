package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.database.Schema;
import at.ac.univie.isc.asio.engine.sql.H2SchemaProvider;
import at.ac.univie.isc.asio.engine.sql.MySqlSchemaProvider;
import com.google.common.base.Supplier;
import rx.Observable;
import rx.Subscriber;

public final class MetadataService {
  private final AtosMetadataService repository;
  private final boolean contactRemote;

  public MetadataService(final AtosMetadataService repository, final boolean contactRemote) {
    this.repository = repository;
    this.contactRemote = contactRemote;
  }

  public Observable<DatasetMetadata> fetch(final Schema schema) {
    if (contactRemote) {
      return Observable.create(new Observable.OnSubscribe<DatasetMetadata>() {
        @Override
        public void call(final Subscriber<? super DatasetMetadata> subscriber) {
          try {
            final DatasetMetadata result = repository.fetchMetadataForId(schema.identifier().toString());
            subscriber.onNext(result);
            subscriber.onCompleted();
          } catch (MetadataNotFound | RepositoryFailure failure) {
            subscriber.onError(failure);
          }
        }
      });
    } else {
      return Observable.from(StaticMetadata.NOT_AVAILABLE);
    }
  }

  public Observable<SqlSchema> relationalSchema(final Schema schema) {
    return Observable.create(new Observable.OnSubscribe<SqlSchema>() {
      @Override
      public void call(final Subscriber<? super SqlSchema> subscriber) {
        final Supplier<SqlSchema> provider;
        if (schema.getJdbcPool().getJdbcUrl().startsWith("jdbc:mysql")) {
          provider = new MySqlSchemaProvider(schema.getJdbcPool());
        } else {
          provider = new H2SchemaProvider(schema.getJdbcPool());
        }
        subscriber.onNext(provider.get());
        subscriber.onCompleted();
      }
    });
  }
}
