package uk.org.ogsadai.activity.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import uk.org.ogsadai.activity.RequestDescriptor;
import uk.org.ogsadai.activity.pipeline.ActivityPipeline;
import uk.org.ogsadai.activity.request.OGSADAIRequestConfiguration;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.authorization.SecurityContext;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.RequestProcessingException;
import uk.org.ogsadai.exception.RequestTerminatedException;
import uk.org.ogsadai.monitoring.MonitoringFramework;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;
import at.ac.univie.isc.asio.DatasetOperationTracker;

import com.google.common.base.Optional;

/**
 * Route filtered OGSADAI request events and errors to active
 * {@link DatasetOperationTracker}s.
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public class RequestEventRouter implements MonitoringFramework, RequestListener {

	private final ConcurrentMap<ResourceID, DatasetOperationTracker> trackers;

	public RequestEventRouter() {
		trackers = new ConcurrentHashMap<>();
	}

	/**
	 * Atomically associate the given request id with the given
	 * {@link DatasetOperationTracker}.
	 * 
	 * @param requestId
	 *            to be tracked
	 * @param callback
	 *            routing target for events with requestId
	 */
	public void track(final ResourceID requestId,
			final DatasetOperationTracker callback) {
		final DatasetOperationTracker former = trackers.putIfAbsent(requestId,
				callback);
		if (former != null) {
			throw new IllegalArgumentException("request [" + requestId
					+ "] already tracked");
		}
	}

	/**
	 * Do not track the given requestId anymore.
	 * 
	 * @param requestId
	 *            that will not be tracked anymore
	 * @return the tracker that was associated with the given id, if there was
	 *         one
	 */
	public Optional<DatasetOperationTracker> stopTracking(
			final ResourceID requestId) {
		final DatasetOperationTracker tracker = trackers.remove(requestId);
		return Optional.fromNullable(tracker);
	}

	/**
	 * Forward interesting request statuses.
	 */
	@Override
	public void requestExecutionStatusEvent(final ResourceID requestId,
			final RequestExecutionStatus status) {
		final DatasetOperationTracker tracker = trackers.get(requestId);
		if (tracker != null) {
			if (forward(status, tracker)) {
				stopTracking(requestId);
			}
		}
	}

	/**
	 * Forward the error.
	 */
	@Override
	public void requestErrorEvent(final ResourceID requestID,
			final DAIException cause) {
		final DatasetOperationTracker tracker = trackers.get(requestID);
		if (tracker != null) {
			forwardError(cause, tracker);
		}
	}

	@Override
	public void registerListeners(final RequestDescriptor request,
			final OGSADAIRequestConfiguration context) {
		final ResourceID requestId = context.getRequestID();
		if (trackers.containsKey(requestId)) {
			context.registerActivityListener(new ActivityErrorListener(this,
					requestId));
		}
	}

	/**
	 * stop tracking of any currently tracked request.
	 */
	@Override
	public void clear() {
		trackers.clear();
	}

	/**
	 * Examine the request status and forward appropriately.
	 * 
	 * @param status
	 *            of request
	 * @param tracker
	 *            of request
	 * @return true if the request processing ended
	 */
	private boolean forward(final RequestExecutionStatus status,
			final DatasetOperationTracker tracker) {
		boolean terminal = false;
		if (status == RequestExecutionStatus.COMPLETED) {
			tracker.complete();
			terminal = true;
		} else if (status == RequestExecutionStatus.TERMINATED) {
			forwardError(new RequestTerminatedException(), tracker);
			terminal = true;
		} else if (status == RequestExecutionStatus.COMPLETED_WITH_ERROR
				|| status == RequestExecutionStatus.ERROR) {
			// this should be caught beforehand by activity listeners
			final RequestProcessingException cause = new RequestProcessingException(
					new IllegalStateException(
							"request failed with unknown error"));
			forwardError(cause, tracker);
			terminal = true;
		}
		return terminal;
	}

	/**
	 * Notify the given tracker of an error
	 * 
	 * @param cause
	 *            of error
	 * @param tracker
	 *            to be notified
	 */
	private void forwardError(final DAIException cause,
			final DatasetOperationTracker tracker) {
		tracker.fail(cause);
	}

	// ignored request events

	@Override
	public void newRequestEvent(final ResourceID requestID,
			final SecurityContext securityContext) {}

	@Override
	public void requestWorkflowEvent(final ResourceID requestID,
			final Workflow workflow) {}

	@Override
	public void requestPipelineEvent(final ResourceID requestID,
			final ActivityPipeline pipeline) {}

	// required no-op implementation of MonitoringFramwork

	@Deprecated
	@Override
	public void addActivityListener(final ActivityListener listener) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public void addPipeListener(final PipeListener listener) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public void removeActivityListener(final ActivityListener listener) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public void removePipeListener(final PipeListener listener) {
		throw new UnsupportedOperationException();
	}
}
