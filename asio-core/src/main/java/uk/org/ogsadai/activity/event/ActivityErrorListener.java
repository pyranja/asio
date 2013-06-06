package uk.org.ogsadai.activity.event;

import javax.annotation.concurrent.ThreadSafe;

import uk.org.ogsadai.activity.Activity;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.resource.ResourceID;

/**
 * Forward activity errors to a {@link RequestListener} using the set request
 * id.
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public class ActivityErrorListener implements ActivityListener {

	private final RequestListener delegate;
	private final ResourceID requestId;

	public ActivityErrorListener(final RequestListener delegate,
			final ResourceID requestId) {
		this.delegate = delegate;
		this.requestId = requestId;
	}

	@Override
	public void error(final Activity source, final DAIException cause) {
		delegate.requestErrorEvent(requestId, cause);
	}

	// all other activity events are ignored

	@Override
	public void pending(final Activity source) {}

	@Override
	public void processing(final Activity source) {}

	@Override
	public void terminated(final Activity source) {}

	@Override
	public void completed(final Activity source) {}

	@Override
	public void warning(final Activity source, final Warning warning) {}

	@Override
	public void otherEvent(final Activity source, final EventDetails details) {}

}
