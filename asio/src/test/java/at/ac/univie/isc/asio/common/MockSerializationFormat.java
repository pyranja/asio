package at.ac.univie.isc.asio.common;

import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.net.MediaType;

public final class MockSerializationFormat implements SerializationFormat {
	@Override
	public MediaType asMediaType() {
		return MediaType.create("application", "test");
	}
}