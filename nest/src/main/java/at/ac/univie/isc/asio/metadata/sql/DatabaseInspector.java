package at.ac.univie.isc.asio.metadata.sql;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.SqlSchema;
import rx.Observable;
import rx.Subscriber;

import javax.sql.DataSource;

/**
 * Reactive adapter for relational schema services.
 */
public final class DatabaseInspector {
  private final Schema schema;
  private final RelationalSchemaService service;

  public static DatabaseInspector from(final Schema schema, final String jdbcUrl, final DataSource pool) {
    final RelationalSchemaService service;
    if (jdbcUrl.startsWith("jdbc:mysql:")) {
      service = new MysqlSchemaService(pool);
    } else if (jdbcUrl.startsWith("jdbc:h2:")) {
      service = new H2SchemaService(pool);
    } else {
      throw new IllegalStateException(jdbcUrl + " not supported");
    }
    return new DatabaseInspector(schema, service);
  }

  private DatabaseInspector(final Schema schema, final RelationalSchemaService service) {
    this.schema = schema;
    this.service = service;
  }

  public Observable<SqlSchema> definition() {
    return Observable.create(new Observable.OnSubscribe<SqlSchema>() {
      @Override
      public void call(final Subscriber<? super SqlSchema> subscriber) {
        try {
          subscriber.onNext(service.explore(schema));
          subscriber.onCompleted();
        } catch (final Exception error) {
          subscriber.onError(error);
        }
      }
    });
  }
}
