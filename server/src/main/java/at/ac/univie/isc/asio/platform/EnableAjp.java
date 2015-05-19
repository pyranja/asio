/*
 * #%L
 * asio server
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
