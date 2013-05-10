package at.ac.univie.isc.asio.ogsadai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.event.CompletionCallback;

public class MockCompletionCallback implements CompletionCallback {

	/* slf4j-logger */
	final static Logger log = LoggerFactory
			.getLogger(MockCompletionCallback.class);

	@Override
	public void complete() {
		log.info("EVENT : complete");
	}

	@Override
	public void fail(final Exception cause) {
		log.error("EVENT : fail", cause);
	}

}
