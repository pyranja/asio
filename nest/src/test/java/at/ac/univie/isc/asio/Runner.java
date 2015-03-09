package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.sql.Database;

/**
 * Start with fixed port
 */
public class Runner {
  public static void main(String[] args) {
    Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));
    Nest.application().logStartupInfo(true).run("--server.port=8080", "--asio.feature.vph-uri-auth=false");
  }
}
