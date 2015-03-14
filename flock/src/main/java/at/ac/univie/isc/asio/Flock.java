package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.insight.EventStreamServlet;
import at.ac.univie.isc.asio.jaxrs.AppSpec;
import at.ac.univie.isc.asio.security.*;
import at.ac.univie.isc.asio.web.SslFixListener;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.spring.SpringResourceFactory;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import javax.servlet.DispatcherType;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.RuntimeDelegate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Import(Config.class)
@EnableAutoConfiguration
public class Flock {
  public static final SpringApplicationBuilder APPLICATION =
      new SpringApplicationBuilder(Flock.class)
          .web(true)
          .headless(true)
          .logStartupInfo(true)
          .showBanner(false);

  public static void main(String[] args) throws Exception {
    Daemonize.current().process();
    APPLICATION.application().run(args);
  }

  // === web components ============================================================================

  @Bean(destroyMethod = "shutdown")
  public Bus cxf() {
    return new SpringBus();
  }

  @Bean(destroyMethod = "stop")
  @DependsOn("cxf")
  public Server jaxrsServer() {
    final JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance()
        .createEndpoint(AppSpec.create(FlockResource.class), JAXRSServerFactoryBean.class);
    factory.setResourceProvider(FlockResource.class, flockResourceProvider());
    factory.getFeatures().add(new LoggingFeature());  // TODO make configurable
    return factory.create();
  }

  @Bean
  public ResourceProvider flockResourceProvider() {
    return new SpringResourceFactory("flockResource");
  }

  @Bean
  @DependsOn("cxf")
  public ServletRegistrationBean dispatcherServlet() {
    final ServletRegistrationBean cxf = new ServletRegistrationBean(new CXFServlet(), "/*");
    cxf.setLoadOnStartup(1);
    cxf.setAsyncSupported(true);
    final Map<String, String> params = new HashMap<>();
    params.put("static-resources-list", "/static/.*");
    params.put("static-welcome-file", "/index.html");
    params.put("redirects-list", "/insight/events");
    params.put("redirect-servlet-name", "event-stream");
    params.put("redirect-attributes", "javax.servlet.include.request_uri");
    cxf.setInitParameters(params);
    cxf.setName("cxf-dispatch");
    return cxf;
  }

  @Bean
  public ServletRegistrationBean eventServlet() {
    final ServletRegistrationBean events =
        new ServletRegistrationBean(new EventStreamServlet(), "/insight/events", "/insight/events/*");
    events.setLoadOnStartup(-1);
    events.setAsyncSupported(true);
    events.setName("event-stream");
    return events;
  }

  @Bean
  public FilterRegistrationBean authFilter() {
    final AdaptAuthorizationFilter filter = new AdaptAuthorizationFilter(
        FixedAuthorityFinder.create(Role.READ),
        TranslateToServletContainerAuthorization.newInstance()
    );
    final FilterRegistrationBean auth = new FilterRegistrationBean(filter);
    auth.setDispatcherTypes(DispatcherType.REQUEST);
    auth.setUrlPatterns(Arrays.asList("/*"));
    auth.setAsyncSupported(true);
    auth.setName("auth-filter");
    auth.setOrder(Priorities.AUTHENTICATION);
    return auth;
  }

  @Bean
  public ServletListenerRegistrationBean<SslFixListener> sslFixListener() {
    return new ServletListenerRegistrationBean<>(new SslFixListener());
  }
}
