package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.sql.Database;
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
    Nest.application()
        .profiles("test")
        .properties("server.ssl.key-store=nest/src/test/resources/keystore")
        .logStartupInfo(true)
        .run(extend(args));
  }

  public static String[] extend(final String[] args) {
    final List<String> arguments = Lists.newArrayList(args);
    arguments.add("--server.port=8443");
    arguments.add("--logging.level.=INFO");
    return arguments.toArray(new String[arguments.size()]);
  }
}
