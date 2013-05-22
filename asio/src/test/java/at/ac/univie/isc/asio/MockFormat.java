package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.net.MediaType;

public final class MockFormat implements SerializationFormat {

	public static final javax.ws.rs.core.MediaType MOCK_CONTENT_TYPE = javax.ws.rs.core.MediaType
			.valueOf("application/test");

	public static final MediaType MOCK_MIME = MediaType.create("application",
			"test");

	@Override
	public MediaType asMediaType() {
		return MOCK_MIME;
	}

	// XXX needs to be configurable ?
	@Override
	public boolean applicableOn(final Action type) {
		return true;
	}
}