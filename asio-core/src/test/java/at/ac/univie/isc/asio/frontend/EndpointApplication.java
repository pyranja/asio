package at.ac.univie.isc.asio.frontend;

import static com.google.common.collect.ImmutableSet.builder;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mockito.Matchers;
import org.mockito.Mockito;

import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.common.RandomIdGenerator;
import at.ac.univie.isc.asio.coordination.EngineSpec.Type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Manual wiring and configuration of the endpoints for test deployments in a standalone servlet
 * container.
 * 
 * @author Chris Borckholder
 */
@ApplicationPath("/")
public class EndpointApplication extends Application {

  private final Set<AbstractEndpoint> endpoints;
  private final EngineAdapter mockEngine;

  EndpointApplication() {
    super();
    final FormatSelector selector =
        new FormatSelector(ImmutableSet.of(MockFormat.ALWAYS_APPLICABLE,
            MockFormat.NEVER_APPLICABLE), new VariantConverter());
    final EngineAdapter selectingAdapter = new EngineAdapter(null, selector);
    mockEngine = Mockito.spy(selectingAdapter);
    endpoints = createEndpoints();
  }

  private Set<AbstractEndpoint> createEndpoints() {
    final AsyncProcessor processor =
        new AsyncProcessor(MoreExecutors.sameThreadExecutor(), new VariantConverter());
    final OperationFactory factory =
        new OperationFactory(RandomIdGenerator.withPrefix("integration"));
    final EngineSelector registry = Mockito.mock(EngineSelector.class);
    Mockito.when(registry.select(Matchers.any(Type.class))).thenReturn(Optional.of(mockEngine));
    return ImmutableSet.of(new QueryEndpoint(registry, processor, factory, Type.SQL),
        new SchemaEndpoint(registry, processor, factory, Type.SQL), new UpdateEndpoint(registry,
            processor, factory, Type.SQL));
  }

  @Override
  public Set<Object> getSingletons() {
    return builder().addAll(endpoints).add(new DatasetExceptionMapper()).build();
  }

  public void resetMocks() {
    Mockito.reset(mockEngine);
  }

  /**
   * @return the mockito mock engine used by the set up endpoints
   */
  public EngineAdapter getMockEngine() {
    return mockEngine;
  }
}
