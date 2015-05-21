/*
 * #%L
 * asio cli
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
  static final String CLI_PROPERTIES = "cli.properties";
  static final String LOGGING_PROPERTIES = "logging.properties";

  /**
   * Run the client with parameters from the command line.
   */
  public static void main(String[] args) throws Exception {
    try {
      try (final InputStream stream = Resources.getResource(LOGGING_PROPERTIES).openStream()) {
        LogManager.getLogManager().readConfiguration(stream);
      }

      final Properties props = new PropertyHierarchy()
          .loadEmbedded(CLI_PROPERTIES)
          .loadSystem()
          .loadExternal(CLI_PROPERTIES)
          .parseCommandLine(args)
          .get();

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
