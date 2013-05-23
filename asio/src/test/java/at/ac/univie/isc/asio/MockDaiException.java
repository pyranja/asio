package at.ac.univie.isc.asio;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.ErrorID;

public class MockDaiException extends DAIException {
	private static final long serialVersionUID = 1L;

	public MockDaiException() {
		this("MOCK dai exception");
	}

	public MockDaiException(final String message) {
		super(ErrorID.NON_INTERNATIONALIZED_MESSAGE, new Object[] { message });
	}

}