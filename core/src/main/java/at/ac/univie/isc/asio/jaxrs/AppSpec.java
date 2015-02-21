package at.ac.univie.isc.asio.jaxrs;

import com.google.common.collect.ImmutableMap;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Define resources in a JAX-RS application.
 */
@ApplicationPath("/")
public final class AppSpec extends Application {
  /**
   * Define application with given prototype resources and default providers.
   * @param resources prototype resources
   * @return application with default configuration
   */
  public static AppSpec create(final Class<?>... resources) {
    final JSONProvider json = new JSONProvider();
    json.setNamespaceMap(ImmutableMap.of("http://isc.univie.ac.at/2014/asio", "asio"));
    json.setProduceMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, DatasetExceptionMapper.ERROR_TYPE.toString()));
    return new AppSpec(Arrays.asList(resources))
        .singleton(new ContentNegotiationOverrideFilter())
        .singleton(new ContentNegotiationDefaultsFilter())
        .singleton(new DatasetExceptionMapper())
        .singleton(json);
  }

  private final Set<Class<?>> prototypes = new HashSet<>();
  private final Set<Object> singletons = new HashSet<>();

  private AppSpec(final Collection<Class<?>> resources) {
    this.prototypes.addAll(resources);
  }

  @Override
  public Set<Class<?>> getClasses() {
    return prototypes;
  }

  @Override
  public Set<Object> getSingletons() {
    return singletons;
  }

  /**
   * @param clazz add the given class as prototype resource
   * @return this application
   */
  public AppSpec prototype(final Class<?> clazz) {
    prototypes.add(clazz);
    return this;
  }

  /**
   * @param instance add the given instance as singleton resource
   * @return this application
   */
  public AppSpec singleton(final Object instance) {
    singletons.add(instance);
    return this;
  }
}
