package at.ac.univie.isc.asio.platform;

import at.ac.univie.isc.asio.Scope;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Add an AJP connector to the embedded tomcat with default settings.
 */
@Configuration
@Profile("ajp")
public class EnableAjp {
  private static final Logger log = getLogger(EnableAjp.class);

  @Bean
  public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
    final Connector connector = ajpConnector();
    log.info(Scope.SYSTEM.marker(), "adding AJP/1.3 connector listening to {}", connector.getPort());
    final TomcatEmbeddedServletContainerFactory factory =
        new TomcatEmbeddedServletContainerFactory();
    factory.getAdditionalTomcatConnectors().add(connector);
    return factory;
  }

  @Bean
  @ConfigurationProperties("ajp")
  public Connector ajpConnector() {
    return new Connector("AJP/1.3");
  }
}
