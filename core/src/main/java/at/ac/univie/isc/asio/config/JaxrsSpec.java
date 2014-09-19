package at.ac.univie.isc.asio.config;

import at.ac.univie.isc.asio.jaxrs.AcceptTunnelFilter;
import at.ac.univie.isc.asio.jaxrs.ContentNegotiationDefaultsFilter;
import at.ac.univie.isc.asio.jaxrs.DatasetExceptionMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/")
public class JaxrsSpec extends Application {

  public static JaxrsSpec create(final Class<?>... resources) {
    return new JaxrsSpec(ImmutableSet.copyOf(resources));
  }

  private final Set<Class<?>> resources;

  private JaxrsSpec(final Set<Class<?>> resources) {
    this.resources = resources;
  }

  @Override
  public Set<Class<?>> getClasses() {
    return resources;
  }

  @Override
  public Set<Object> getSingletons() {
    final ImmutableSet.Builder<Object> providers = ImmutableSet.builder();
    providers.addAll(filters());
    providers.addAll(converters());
    return providers.build();
  }

  private ImmutableSet<Object> converters() {
    final JSONProvider json = new JSONProvider();
    json.setNamespaceMap(ImmutableMap.of("http://isc.univie.ac.at/2014/asio", "asio"));
    return ImmutableSet.of(new DatasetExceptionMapper(), json);
  }

  private ImmutableSet<ContainerRequestFilter> filters() {
    return ImmutableSet.of(new AcceptTunnelFilter()
        , new ContentNegotiationDefaultsFilter()
    );
  }
}
