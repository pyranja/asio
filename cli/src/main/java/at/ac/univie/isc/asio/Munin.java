package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.munin.Pigeon;
import at.ac.univie.isc.asio.security.AllowAllVerifier;
import at.ac.univie.isc.asio.security.NoopTrustManager;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.GeneralSecurityException;

@Configuration
@ComponentScan("at.ac.univie.isc.asio.munin")
@EnableConfigurationProperties(MuninSettings.class)
@Import({PropertyPlaceholderAutoConfiguration.class, JacksonAutoConfiguration.class})
public class Munin {

  @Autowired
  private MuninSettings config;

  public static void main(String[] args) {
    new SpringApplicationBuilder(Munin.class)
        .web(false)
        .logStartupInfo(false)
        .showBanner(false)
        .run(args);
  }

  @Bean
  public Appendable console() {
    return System.out;
  }

  @Bean
  public Pigeon pigeon(final Client client) {
    return Pigeon.connectTo(client.target(config.getServerAddress()));
  }

  @Bean
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
