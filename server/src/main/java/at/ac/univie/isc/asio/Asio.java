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
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.Connector;
import at.ac.univie.isc.asio.engine.EngineRouter;
import at.ac.univie.isc.asio.engine.EventfulConnector;
import at.ac.univie.isc.asio.engine.ReactiveInvoker;
import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.metadata.AtosMetadataRepository;
import at.ac.univie.isc.asio.metadata.DescriptorConversion;
import at.ac.univie.isc.asio.metadata.DescriptorService;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.platform.CurrentTime;
import at.ac.univie.isc.asio.platform.Launcher;
import at.ac.univie.isc.asio.security.Authorizer;
import at.ac.univie.isc.asio.spring.EventBusAutoRegistrar;
import at.ac.univie.isc.asio.spring.ExplicitWiring;
import at.ac.univie.isc.asio.spring.JerseyLogInitializer;
import at.ac.univie.isc.asio.spring.SpringAutoFactory;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Ticker;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.security.concurrent.DelegatingSecurityContextScheduledExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;
import org.springframework.web.context.WebApplicationContext;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = {
    ManagementSecurityAutoConfiguration.class,
    DataSourceAutoConfiguration.class
})
@EnableConfigurationProperties({AsioSettings.class})
@ComponentScan(
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = SpringAutoFactory.class)
    , excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = ExplicitWiring.class)
)
public class Asio {

  public static void main(String[] args) {
    Launcher.currentProcess().daemonize();
    Locale.setDefault(Locale.ENGLISH);
    application().run(args);
  }

  static SpringApplicationBuilder application() {
    return new SpringApplicationBuilder(Asio.class).web(true).showBanner(false)
        .listeners(new JerseyLogInitializer());
  }

  @Bean
  public static EventBusAutoRegistrar eventBusAutoRegistrar(final EventBus eventBus) {
    return new EventBusAutoRegistrar(eventBus);
  }

  @Autowired
  private AsioSettings config;

  @Bean
  public Connector connector(final EngineRouter router,
                             final Authorizer authorizer,
                             final Emitter emitter) {
    final ReactiveInvoker invoker = ReactiveInvoker.from(router, Schedulers.io(), authorizer);
    return EventfulConnector.around(emitter, invoker);
  }

  @Bean
  @ConditionalOnProperty(AsioFeatures.VPH_METADATA)
  public DescriptorService descriptorService(final Client http) {
    final WebTarget endpoint = http.target(config.getMetadataRepository());
    final AtosMetadataRepository atos = new AtosMetadataRepository(endpoint);
    return new DescriptorService() {
      @Override
      public Observable<SchemaDescriptor> metadata(final URI identifier) {
        return atos.findByLocalId(identifier.toString()).map(DescriptorConversion.asFunction());
      }
   };
  }

  @Bean(destroyMethod = "close")
  @Lazy
  public Client httpClient() {
    return ClientBuilder.newClient();
  }

  @Bean
  public EventBus eventBus(final ScheduledExecutorService workerPool) {
    return new AsyncEventBus("asio-events", workerPool);
  }

  @Bean(destroyMethod = "shutdown")
  public ScheduledExecutorService workerPool() {
    final ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("asio-worker-%d").build();
    final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(factory);
    final DelegatingSecurityContextScheduledExecutorService secured =
        new DelegatingSecurityContextScheduledExecutorService(executor);
    return secured;
  }

  public static final IdGenerator ID_GENERATOR = new AlternativeJdkIdGenerator();

  @Bean
  @Scope(WebApplicationContext.SCOPE_REQUEST)
  public Correlation correlation() {
    return Correlation.valueOf(ID_GENERATOR.generateId().toString());
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.INTERFACES)
  public SecurityContext currentUser() {
    return SecurityContextHolder.getContext();
  }

  @Bean
  public Timeout globalTimeout() {
    return Timeout.from(config.timeout, TimeUnit.MILLISECONDS);
  }

  @Bean
  public Ticker time() {
    return CurrentTime.instance();
  }
}
