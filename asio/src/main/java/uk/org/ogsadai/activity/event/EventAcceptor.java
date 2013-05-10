package uk.org.ogsadai.activity.event;

import static uk.org.ogsadai.resource.request.RequestExecutionStatus.COMPLETED;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.ERROR;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.TERMINATED;

import java.util.concurrent.atomic.AtomicBoolean;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.RequestProcessingException;
import uk.org.ogsadai.exception.RequestTerminatedException;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;

/**
 * Inspect request events (status changes and errors) and decide whether to
 * notify the given {@link CompletionCallback}.
 * 
 * @author Chris Borckholder
 */
public class EventAcceptor {

	private final CompletionCallback delegate;
	private final AtomicBoolean stopTrackingOnNextError;
	private final AtomicBoolean hasError;

	EventAcceptor(final CompletionCallback delegate) {
		this.delegate = delegate;
		stopTrackingOnNextError = new AtomicBoolean(false);
		hasError = new AtomicBoolean(false);
	}

	/**
	 * Inspect the new state after a change and notify if required.
	 * 
	 * @param status
	 *            of request
	 * @return true if the request should not be tracked anymore
	 */
	public boolean handleStateAndStop(final RequestExecutionStatus status) {
		boolean stopTracking = false;
		if (status.hasFinished()) {
			if (status == COMPLETED) {
				delegate.complete();
				stopTracking = true;
			} else if (status == TERMINATED) {
				delegate.fail(new RequestTerminatedException());
				stopTracking = true;
			} else if (status == ERROR) {
				stopTracking = stopTrackingOnNextError.getAndSet(true)
						|| hasError.get();
			} else {
				delegate.fail(new RequestProcessingException(
						new IllegalStateException("unknown error")));
				throw new AssertionError(
						"received unexpected terminal request status " + status);
			}
		}
		return stopTracking;
	}

	/**
	 * Notify the callback of the request error.
	 * 
	 * @param cause
	 *            of request failure
	 * @return true if the request should not be tracked anymore
	 */
	public boolean handleErrorAndStop(final DAIException cause) {
		hasError.set(true);
		delegate.fail(cause);
		return stopTrackingOnNextError.get();
	}

	/**
	 * @return the {@link CompletionCallback} this acceptor delegates to.
	 */
	public CompletionCallback getDelegate() {
		return delegate;
	}
}
