package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.database.Schema;
import rx.Observable;
import rx.util.async.Async;

import java.util.concurrent.Callable;

public final class MetadataService {
  private final AtosMetadataService repository;
  private final boolean contactRemote;

  public MetadataService(final AtosMetadataService repository, final boolean contactRemote) {
    this.repository = repository;
    this.contactRemote = contactRemote;
  }

  public Observable<DatasetMetadata> fetch(final Schema schema) {
    if (contactRemote) {
      return Async.fromCallable(new Callable<DatasetMetadata>() {
        @Override
        public DatasetMetadata call() throws Exception {
          return repository.fetchMetadataForId(schema.identifier().toString());
        }
      });
    } else {
      return Observable.from(StaticMetadata.NOT_AVAILABLE);
    }
  }

  public Observable<SqlSchema> relationalSchema(final Schema schema) {
    return Async.fromCallable(new Callable<SqlSchema>() {
      @Override
      public SqlSchema call() throws Exception {
        return schema.relationalModel().fetch();
      }
    });
  }
}
