package at.ac.univie.isc.asio;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class MockIntegrationTest {

	@Test
	public void dummy() throws Exception {
		System.err.println("INTEGRATION TEST");
		fail("MOCK_INTEGRATION_FAILURE");
	}
}
