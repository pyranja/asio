package at.ac.univie.isc.asio.test;

import at.ac.univie.isc.asio.DatasetOperation.OperationType;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;

import com.google.common.net.MediaType;

public final class MockSerializationFormat implements SerializationFormat {
	@Override
	public MediaType asMediaType() {
		return MediaType.create("application", "test");
	}

	// XXX needs to be configurable ?
	@Override
	public boolean applicableOn(final OperationType type) {
		return true;
	}
}