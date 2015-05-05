package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.Pigeon;
import at.ac.univie.isc.asio.security.AllowAllVerifier;
import at.ac.univie.isc.asio.security.NoopTrustManager;
import at.ac.univie.isc.asio.tool.PropertyHierarchy;
import com.google.common.io.Resources;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Command line runner.
 */
public final class Munin {
  static final String EMBEDDED_PROPERTIES = "cli.properties";
  static final String LOGGING_PROPERTIES = "logging.properties";

  /**
   * Run the client with parameters from the command line.
   */
  public static void main(String[] args) throws Exception {
    try {
      try (final InputStream stream = Resources.getResource(LOGGING_PROPERTIES).openStream()) {
        LogManager.getLogManager().readConfiguration(stream);
      }

      final Properties props =
          new PropertyHierarchy(EMBEDDED_PROPERTIES).parseCommandLine(args).get();

      if (Boolean.parseBoolean(props.getProperty("debug"))) {
        Logger.getLogger("at.ac.univie.isc.asio").setLevel(Level.FINE);
      }

      System.exit(new Munin(Settings.fromProperties(props)).run(args));

    } catch (final Exception e) {
      System.err.println("[ERROR] fatal - " + e.getMessage());
      System.exit(1);
    }
  }

  private final Settings config;

  Munin(final Settings config) {
    this.config = config;
  }

  public int run(final String[] args) throws Exception {
    final Client client = httpClient();
    try {
      final Map<String, Command> mappings = Command.Create.all(console(), pigeon(client));
      final Controller controller = new Controller(console(), mappings);
      controller.run(args);
      return controller.getExitCode();
    } finally {
      client.close();
    }
  }

  public Appendable console() {
    return System.out;
  }

  public Pigeon pigeon(final Client client) {
    return Pigeon.connectTo(client.target(config.getServerAddress()));
  }

  public Client httpClient() throws GeneralSecurityException {
    final Client client;
    if (config.isInsecureConnection()) {
      final SSLContext ssl = SSLContext.getInstance("TLS");
      ssl.init(null, NoopTrustManager.asArray(), null);
      client = ClientBuilder.newBuilder()
          .hostnameVerifier(AllowAllVerifier.instance())
          .sslContext(ssl)
          .build();
    } else {
      client = ClientBuilder.newClient();
    }
    final HttpAuthenticationFeature authentication =
        HttpAuthenticationFeature.basic(config.getUsername(), config.getPassword());
    client.register(authentication);
    return client;
  }
}
