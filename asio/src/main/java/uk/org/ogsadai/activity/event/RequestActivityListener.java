package uk.org.ogsadai.activity.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.Activity;
import uk.org.ogsadai.activity.ActivityName;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.resource.ResourceID;

public class RequestActivityListener implements ActivityListener {

	/* slf4j-logger */
	final static Logger log = LoggerFactory
			.getLogger(RequestActivityListener.class);

	private final ResourceID request;

	RequestActivityListener(final ResourceID request) {
		super();
		this.request = request;
	}

	private ActivityName name(final Activity source) {
		return source.getActivityDescriptor().getActivityName();
	}

	@Override
	public void completed(final Activity source) {
		log.info("{} - {} completed", request, name(source));
	}

	@Override
	public void error(final Activity source, final DAIException error) {
		log.error("{} - {} failed with {}", request, name(source),
				error.getLocalizedMessage(), error);
	}

	@Override
	public void otherEvent(final Activity source, final EventDetails details) {
		// ignore
		log.warn("unknown event {}", details);
	}

	@Override
	public void pending(final Activity source) {
		log.info("{} - {} pending", request, name(source));
	}

	@Override
	public void processing(final Activity source) {
		log.info("{} - {} processing", request, name(source));
	}

	@Override
	public void terminated(final Activity source) {
		log.error("{} - {} terminated", request, name(source));
	}

	@Override
	public void warning(final Activity source, final Warning warning) {
		log.warn("{} - {} warning {}", request, name(source),
				warning.getDescription());
	}
}
