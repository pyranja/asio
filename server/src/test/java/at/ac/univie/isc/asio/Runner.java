package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.atos.FakeAtosService;
import at.ac.univie.isc.asio.integration.IntegrationDatabase;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.TransientPath;
import at.ac.univie.isc.asio.platform.FileSystemConfigStore;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

/**
 * Start with fixed port
 */
public class Runner {
  /**
   * active profiles
   */
  static final List<String> profiles = Lists.newArrayList("brood", "dev");

  public static void main(final String[] args) throws IOException {
    final Database database = IntegrationDatabase.defaultCatalog()
        .auto();
    //        .h2InMemory();

    database.execute(Classpath.read("sql/database.integration.sql"));
    database.execute(Classpath.read("sql/gui.integration.sql"));
    profiles.add(database.getType());

    FakeAtosService.attachTo(HttpServer.create("atos-fake").enableLogging()).start(8401);

    try (final TransientPath keystore = TransientPath.file(Classpath.toArray("keystore.integration")).init();
         final TransientPath workingDirectory = TransientPath.folder().init()) {

      workingDirectory.add(
          FileSystemConfigStore.STORE_FOLDER.resolve("public##config"),
          Classpath.load("config.integration.ttl").read());

      Asio.application()
          .profiles(profiles.toArray(new String[profiles.size()]))
          .properties(
              "server.ssl.key-store:" + keystore.path()
              , "defaults.home:" + workingDirectory.path()
          )
          .logStartupInfo(true)
          .run(args);

      System.out.println("  ===  running...  ===  ");
      System.in.read();
    }
  }
}
