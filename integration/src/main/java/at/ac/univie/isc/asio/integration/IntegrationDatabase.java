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

  public Database mysqlIfAvailable() {
    final Database mysql = Database
        .create("jdbc:mysql://localhost:" + port() + "/?allowMultiQueries=true")
        .credentials(username(), password())
        .build();
    if (mysql.isAvailable()) {
      return mysql
          .execute("DROP DATABASE IF EXISTS `" + catalog + "`;")
          .execute("CREATE DATABASE `" + catalog + "`;")
          .switchCatalog(catalog);
    } else {
      return h2InMemory();
    }
  }

  public Database h2InMemory() {
    return Database
        .create("jdbc:h2:mem:public;DB_CLOSE_DELAY=-1;MODE=MYSQL;DATABASE_TO_UPPER=false;")
        .credentials(username(), password())
        .build()
        .execute("CREATE SCHEMA IF NOT EXISTS " + catalog)
        .execute("SET SCHEMA " + catalog)
        ;
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
