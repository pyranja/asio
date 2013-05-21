package at.ac.univie.isc.asio;

import java.net.URI;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up a cxf WebClient before each test and disposes it after the test. If a
 * test fails, the contents of the last response received are logged.
 * 
 * @author Chris Borckholder
 */
public class JaxrsClientProvider extends TestWatcher {

	/* slf4j-logger */
	final static Logger log = LoggerFactory
			.getLogger(JaxrsClientProvider.class);

	private final WebClient client;

	public JaxrsClientProvider(final URI baseAddress) {
		super();
		client = WebClient.create(baseAddress);
	}

	public WebClient getClient() {
		return client;
	}

	@Override
	protected void failed(final Throwable e, final Description description) {
		String responseText = "[no response received]";
		final Response response = client.getResponse();
		if (response != null) {
			responseText = TestUtils.stringify(response);
		}
		log.error("{} failed with {} on\n{}", description, e.getMessage(),
				responseText);
	}

	@Override
	protected void finished(final Description description) {
		client.reset();
	}
}
