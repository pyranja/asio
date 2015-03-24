package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.atos.FakeAtosService;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Start with fixed port
 */
public class Runner {
  public static void main(final String[] args) {
    Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));

    FakeAtosService.attachTo(HttpServer.create("atos-fake").enableLogging()).start(8401);

    Nest.application()
        .profiles("test")
        .properties("server.ssl.key-store=integration/src/main/resources/keystore.integration")
        .logStartupInfo(true)
        .run(extend(args));
  }

  public static String[] extend(final String[] args) {
    final List<String> arguments = Lists.newArrayList(args);
    arguments.add("--server.port=8443");
    arguments.add("--asio.feature.vph-metadata=on");
    arguments.add("--asio.metadata-repository=http://localhost:8401/");
    arguments.add("--logging.level.=INFO");
    arguments.add("--logging.level.at.ac.univie.isc.asio=DEBUG");
    return arguments.toArray(new String[arguments.size()]);
  }
}
