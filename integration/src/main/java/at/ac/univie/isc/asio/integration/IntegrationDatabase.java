/*
 * #%L
 * asio integration
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
package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.sql.Database;

/**
 * Setup a database with the integration reference data.
 */
public final class IntegrationDatabase {
  /** use fixed integration database schema name 'public' */
  public static IntegrationDatabase defaultCatalog() {
    return catalog("PUBLIC");
  }

  public static IntegrationDatabase catalog(final String catalog) {
    return new IntegrationDatabase(catalog);
  }

  private final String catalog;

  IntegrationDatabase(final String catalog) {
    this.catalog = catalog;
  }

  public Database mysql() {
    final Database mysql = Database
        .create("jdbc:mysql://localhost:" + port() + "/?allowMultiQueries=true")
        .credentials(username(), password())
        .build();
    if (mysql.isAvailable()) {
      mysql
          .execute("DROP DATABASE IF EXISTS `" + catalog + "`;")
          .execute("CREATE DATABASE `" + catalog + "`;")
          .switchCatalog(catalog);
    }
    return mysql;
  }

  public Database h2InMemory() {
    return Database
        .create("jdbc:h2:mem:public;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=false;")
        .credentials(username(), password())
        .build()
        // .execute("DROP SCHEMA \"PUBLIC\"")
        .execute("CREATE SCHEMA IF NOT EXISTS " + catalog)
        .execute("SET SCHEMA " + catalog)
        ;
  }

  public Database auto() {
    final Database mysql = mysql();
    if (mysql.isAvailable()) {
      return mysql;
    } else {
      return h2InMemory();
    }
  }

  private String username() {
    return System.getProperty("IT_DB_USERNAME", "root");
  }

  private String password() {
    return System.getProperty("IT_DB_PASSWORD", "change");
  }

  private int port() {
    return Integer.parseInt(System.getProperty("IT_DB_PORT", "3306"));
  }
}
