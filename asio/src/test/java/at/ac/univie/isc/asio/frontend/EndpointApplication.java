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

	EndpointApplication() {
		super();
		mockEngine = Mockito.mock(DatasetEngine.class);
	}

	@Override
	public Set<Object> getSingletons() {
		final SqlQueryEndpoint endpoint = new SqlQueryEndpoint(mockEngine);
		return ImmutableSet.<Object> of(endpoint);
	}

	/**
	 * @return the mockito mock engine used by the set up endpoint
	 */
	public DatasetEngine getMockEngine() {
		return mockEngine;
	}
}
