package at.ac.univie.isc.asio.ogsadai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MockOperationTracker implements DatasetOperationTracker {

	/* slf4j-logger */
	final static Logger log = LoggerFactory
			.getLogger(MockOperationTracker.class);

	@Override
	public void receiving() {
		log.info("EVENT : receiving");
	}

	@Override
	public void complete() {
		log.info("EVENT : complete");
	}

	@Override
	public void fail(final Exception cause) {
		log.error("EVENT : fail", cause);
	}

}
