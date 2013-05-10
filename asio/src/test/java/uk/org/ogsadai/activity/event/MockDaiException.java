package uk.org.ogsadai.activity.event;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.ErrorID;

class MockDaiException extends DAIException {
	private static final long serialVersionUID = 1L;

	MockDaiException(final String message) {
		super(ErrorID.NON_INTERNATIONALIZED_MESSAGE,
				new Object[] { message });
	}

}