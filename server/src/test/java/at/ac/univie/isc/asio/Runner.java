package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.atos.FakeAtosService;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.TransientPath;
import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.HttpServer;

import java.io.IOException;

/**
 * Start with fixed port
 */
public class Runner {
  public static void main(final String[] args) throws IOException {
    Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));

    FakeAtosService.attachTo(HttpServer.create("atos-fake").enableLogging()).start(8401);

    try (final TransientPath keystore = TransientPath.file(Classpath.toArray("keystore.integration")).init();
         final TransientPath workingDirectory = TransientPath.folder().init()) {

      workingDirectory.add(
          FileSystemConfigStore.STORE_FOLDER.resolve("public##config"),
          Classpath.load("config.integration.ttl").read());

      Asio.application()
          .profiles("brood", "dev")
          .properties(
              "server.ssl.key-store:" + keystore.path()
              , "defaults.home:" + workingDirectory.path()
          )
          .logStartupInfo(true)
          .run(args);
    }
  }
}
