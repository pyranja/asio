package at.ac.univie.isc.asio.frontend;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.mockito.Mockito;

import at.ac.univie.isc.asio.DatasetEngine;

import com.google.common.collect.ImmutableSet;

/**
 * Manual wiring and configuration of the endpoints for test deployments in a
 * standalone servlet container.
 * 
 * @author Chris Borckholder
 */
@ApplicationPath("/")
public class EndpointApplication extends Application {

	private final DatasetEngine mockEngine;
	private final SqlQueryEndpoint endpoint;

	EndpointApplication() {
		super();
		mockEngine = Mockito.mock(DatasetEngine.class);
		endpoint = new SqlQueryEndpoint(mockEngine);
	}

	@Override
	public Set<Object> getSingletons() {
		return ImmutableSet.<Object> of(endpoint);
	}

	/**
	 * @return the mockito mock engine used by the set up endpoint
	 */
	public DatasetEngine getMockEngine() {
		return mockEngine;
	}

	/**
	 * @return the endpoint service instance
	 */
	public SqlQueryEndpoint getEndpoint() {
		return endpoint;
	}
}
