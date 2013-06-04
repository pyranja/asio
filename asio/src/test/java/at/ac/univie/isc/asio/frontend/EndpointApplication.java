package at.ac.univie.isc.asio.frontend;

import static com.google.common.collect.ImmutableSet.builder;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mockito.Mockito;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.MockFormat;
import at.ac.univie.isc.asio.common.RandomIdGenerator;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Manual wiring and configuration of the endpoints for test deployments in a
 * standalone servlet container.
 * 
 * @author Chris Borckholder
 */
@ApplicationPath("/")
public class EndpointApplication extends Application {

	private final Set<AbstractEndpoint> endpoints;
	private final DatasetEngine mockEngine;

	EndpointApplication() {
		super();
		mockEngine = Mockito.mock(DatasetEngine.class);
		when(mockEngine.supportedFormats()).thenReturn(
				ImmutableSet.of(MockFormat.ALWAYS_APPLICABLE,
						MockFormat.NEVER_APPLICABLE));
		endpoints = createEndpoints();
	}

	private Set<AbstractEndpoint> createEndpoints() {
		final EngineAdapter adapter = EngineAdapter.adapt(mockEngine);
		final AsyncProcessor processor = new AsyncProcessor(
				MoreExecutors.sameThreadExecutor(), new VariantConverter());
		final OperationFactory factory = new OperationFactory(
				RandomIdGenerator.withPrefix("integration"));
		return ImmutableSet.of(new QueryEndpoint(adapter, processor, factory),
				new SchemaEndpoint(adapter, processor, factory),
				new UpdateEndpoint(adapter, processor, factory));
	}

	@Override
	public Set<Object> getSingletons() {
		return builder().addAll(endpoints).add(new DatasetExceptionMapper())
				.build();
	}

	/**
	 * @return the mockito mock engine used by the set up queryEndpoint
	 */
	public DatasetEngine getMockEngine() {
		return mockEngine;
	}

	/**
	 * @return all defined endpoints
	 */
	public Set<AbstractEndpoint> getEndpoints() {
		return endpoints;
	}
}
