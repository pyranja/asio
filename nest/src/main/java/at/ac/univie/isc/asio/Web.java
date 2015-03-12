package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.EndpointsResource;
import at.ac.univie.isc.asio.insight.EventResource;
import at.ac.univie.isc.asio.jaxrs.DatasetExceptionMapper;
import at.ac.univie.isc.asio.metadata.MetaResource;
import at.ac.univie.isc.asio.security.AccessDeniedJaxrsHandler;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.tool.ExpandingQNameSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.threetenbp.ThreeTenModule;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Configuration
class Web {
  private static final Logger log = getLogger(Web.class);

  @Value("${spring.jersey.debug:false}")
  private boolean debug;

  @ApplicationPath("/")
  private static class Application extends ResourceConfig { /* just carries annotation */ }

  @Bean
  public ResourceConfig jerseyConfiguration(final ObjectMapper mapper,
                                            final Set<ContainerRequestFilter> filters) {
    final ResourceConfig config = new Application();
    config.setApplicationName("jersey-nest");
    config.register(MetaResource.class);
    config.register(WhoamiResource.class);
    config.register(EndpointsResource.class);
    config.register(EventResource.class);
    config.register(DatasetExceptionMapper.class);
    config.register(AccessDeniedJaxrsHandler.class);
    log.info(Scope.SYSTEM.marker(), "registering jersey filters {}", filters);
    config.registerInstances(filters.toArray());
    log.info(Scope.SYSTEM.marker(), "registering jackson mapper {}", mapper);
    config.registerInstances(new ObjectMapperProvider(mapper));
    if (debug) {
      config.registerInstances(new LoggingFilter());
    }
    return config;
  }

  @Bean
  public FilterRegistrationBean disableHiddenHttpMethodFilter(final HiddenHttpMethodFilter filter) {
    // HiddenHttpMethodFilter consumes POST form payload and breaks jersey parsing downstream
    final FilterRegistrationBean registration = new FilterRegistrationBean(filter);
    registration.setEnabled(false);
    return registration;
  }

  // modules to be picked up by JacksonAutoConfiguration

  @Bean
  public ThreeTenModule threeTenModule() {
    return new ThreeTenModule();
  }

  @Bean
  @Primary
  public ObjectMapper customObjectMapper(final Jackson2ObjectMapperBuilder builder) {
    // create customized from preconfigured builder
    return builder.createXmlMapper(false).serializers(new ExpandingQNameSerializer()).build();
  }

  /**
   * Needed to let jackson jaxrs provider find the spring managed ObjectMapper.
   */
  @Provider
  static final class ObjectMapperProvider implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    ObjectMapperProvider(final ObjectMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public ObjectMapper getContext(final Class<?> type) {
      return mapper;
    }
  }
}
