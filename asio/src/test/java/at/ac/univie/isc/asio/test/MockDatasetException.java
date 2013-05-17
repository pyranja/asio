package at.ac.univie.isc.asio.test;

import at.ac.univie.isc.asio.DatasetException;

public class MockDatasetException extends DatasetException {

	private static final long serialVersionUID = 1L;

	public MockDatasetException() {
		super("MOCK", "test-exception", null);
	}
}
