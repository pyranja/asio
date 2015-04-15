package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.atos.FakeAtosService;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.TransientFile;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.HttpServer;

/**
 * Start with fixed port
 */
public class Runner {
  public static void main(final String[] args) {
    Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));

    FakeAtosService.attachTo(HttpServer.create("atos-fake").enableLogging()).start(8401);

    try (final TransientFile keystore = TransientFile.create(Classpath.load("keystore.integration"))) {
      Asio.application()
          .profiles("brood", "dev")
          .properties("server.ssl.key-store:" + keystore.path())
          .logStartupInfo(true)
          .run(args);
    }
  }
}
