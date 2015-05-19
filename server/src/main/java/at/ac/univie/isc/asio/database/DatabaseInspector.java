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
package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import rx.Observable;
import rx.Subscriber;

import javax.sql.DataSource;

/**
 * Reactive adapter for relational schema services.
 */
public final class DatabaseInspector implements DefinitionService {
  public static DatabaseInspector create(final String jdbcUrl, final DataSource pool) {
    final RelationalSchemaService service;
    if (jdbcUrl.startsWith("jdbc:mysql:")) {
      service = new MysqlSchemaService(pool);
    } else if (jdbcUrl.startsWith("jdbc:h2:")) {
      service = new H2SchemaService(pool);
    } else {
      throw new IllegalStateException(jdbcUrl + " not supported");
    }
    return new DatabaseInspector(service);
  }

  private final RelationalSchemaService service;

  private DatabaseInspector(final RelationalSchemaService service) {
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
}
