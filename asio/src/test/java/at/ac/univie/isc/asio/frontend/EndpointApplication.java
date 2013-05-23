package at.ac.univie.isc.asio.frontend;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mockito.Mockito;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.OperationFactory;
import at.ac.univie.isc.asio.common.RandomIdGenerator;

import com.google.common.collect.ImmutableSet;

/**
 * Manual wiring and configuration of the endpoints for test deployments in a
 * standalone servlet container.
 * 
 * @author Chris Borckholder
 */
@ApplicationPath("/")
public class EndpointApplication extends Application {

	private final QueryEndpoint queryEndpoint;
	private final SchemaEndpoint schemaEndpoint;

	private final DatasetEngine mockEngine;
	private final OperationFactory factory;

	EndpointApplication() {
		super();
		mockEngine = Mockito.mock(DatasetEngine.class);
		factory = new OperationFactory(
				RandomIdGenerator.withPrefix("integration"));
		queryEndpoint = new QueryEndpoint(mockEngine, factory);
		schemaEndpoint = new SchemaEndpoint(mockEngine, factory);
	}

	@Override
	public Set<Object> getSingletons() {
		return ImmutableSet.<Object> of(queryEndpoint, schemaEndpoint);
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
		return ImmutableSet.of(queryEndpoint, schemaEndpoint);
	}

	/**
	 * @return the queryEndpoint service instance
	 */
	public QueryEndpoint getQueryEndpoint() {
		return queryEndpoint;
	}

	/**
	 * @return the schemaEndpoint service instance
	 */
	public SchemaEndpoint getSchemaEndpoint() {
		return schemaEndpoint;
	}
}
