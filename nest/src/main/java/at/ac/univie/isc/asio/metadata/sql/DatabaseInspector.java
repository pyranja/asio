package at.ac.univie.isc.asio.metadata.sql;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.container.DefinitionService;
import rx.Observable;
import rx.Subscriber;

import javax.sql.DataSource;

/**
 * Reactive adapter for relational schema services.
 */
public final class DatabaseInspector implements DefinitionService {
  private final Id schema;
  private final RelationalSchemaService service;

  public static DatabaseInspector nonFixedSchema(final String jdbcUrl, final DataSource pool) {
    final RelationalSchemaService service;
    if (jdbcUrl.startsWith("jdbc:mysql:")) {
      service = new MysqlSchemaService(pool);
    } else if (jdbcUrl.startsWith("jdbc:h2:")) {
      service = new H2SchemaService(pool);
    } else {
      throw new IllegalStateException(jdbcUrl + " not supported");
    }
    return new DatabaseInspector(null, service);
  }

  public static DatabaseInspector from(final Id schema, final String jdbcUrl, final DataSource pool) {
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

  private DatabaseInspector(final Id schema, final RelationalSchemaService service) {
    this.schema = schema;
    this.service = service;
  }

  @Override
  public Observable<SqlSchema> definition(final String name) {
    return Observable.create(new Observable.OnSubscribe<SqlSchema>() {
      @Override
      public void call(final Subscriber<? super SqlSchema> subscriber) {
        try {
          subscriber.onNext(service.explore(Id.valueOf(name)));
          subscriber.onCompleted();
        } catch (final Exception error) {
          subscriber.onError(error);
        }
      }
    });
  }

  public Observable<SqlSchema> definition() {
    return definition(schema.asString());
  }
}
