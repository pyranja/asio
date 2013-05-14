package uk.org.ogsadai.activity.event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import uk.org.ogsadai.activity.RequestDescriptor;
import uk.org.ogsadai.activity.pipeline.ActivityPipeline;
import uk.org.ogsadai.activity.request.OGSADAIRequestConfiguration;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.authorization.SecurityContext;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.monitoring.MonitoringFramework;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;

import com.google.common.base.Optional;

/**
 * Route filtered OGSADAI request completion and errors to active
 * {@link CompletionCallback}s.
 * 
 * <p>
 * If a {@link CompletionCallback} is registered for a request id, any change in
 * the request's status that signals the termination of the request is inspected
 * and the either the {@link CompletionCallback#complete()} or
 * {@link CompletionCallback#fail(Exception)} method are called. As soon as a
 * request reached a terminal state, it will not be tracked anymore.
 * </p>
 * <p>
 * Additionally, if a request id is setup for tracking before the request is
 * created by OGSADAI, appropriate listeners are attached to it, which also
 * listen for errors in its activities. If any activity encounters an error, it
 * is assumed that the request is failing and
 * {@link CompletionCallback#fail(Exception)} is called. Note that if multiple
 * activities encounter errors, {@link CompletionCallback#fail(Exception)} may
 * be called multiple times, until the request terminates.
 * </p>
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public class RequestEventRouter implements MonitoringFramework, RequestListener {

	private final ConcurrentMap<ResourceID, EventAcceptor> handlers;

	public RequestEventRouter() {
		handlers = new ConcurrentHashMap<>();
	}

	/**
	 * Forward interesting request statuses if requestId is tracked.
	 */
	@Override
	public void requestExecutionStatusEvent(final ResourceID requestId,
			final RequestExecutionStatus status) {
		final EventAcceptor acceptor = handlers.get(requestId);
		if (acceptor != null) {
			if (acceptor.handleStateAndStop(status)) {
				stopTracking(requestId);
			}
		}
	}

	/**
	 * Forward the error if requestId is tracked.
	 */
	@Override
	public void requestErrorEvent(final ResourceID requestId,
			final DAIException cause) {
		final EventAcceptor acceptor = handlers.get(requestId);
		if (acceptor != null) {
			if (acceptor.handleErrorAndStop(cause)) {
				stopTracking(requestId);
			}
		}
	}

	/**
	 * Register an activity error listener if the created request is tracked.
	 */
	@Override
	public void registerListeners(final RequestDescriptor request,
			final OGSADAIRequestConfiguration context) {
		final ResourceID requestId = context.getRequestID();
		if (handlers.containsKey(requestId)) {
			context.registerActivityListener(new ActivityErrorListener(this,
					requestId));
		}
	}

	/**
	 * Atomically associate the given request id with the given
	 * {@link CompletionCallback}.
	 * 
	 * @param requestId
	 *            to be tracked
	 * @param callback
	 *            routing target for events with requestId
	 * @throws IllegalArgumentException
	 *             if the given requestId is already tracked
	 */
	public void track(final ResourceID requestId,
			final CompletionCallback callback) {
		checkNotNull(callback, "cannot track with null callback");
		final EventAcceptor acceptor = new EventAcceptor(callback);
		final EventAcceptor former = handlers.putIfAbsent(requestId, acceptor);
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
	public Optional<CompletionCallback> stopTracking(final ResourceID requestId) {
		final EventAcceptor acceptor = handlers.remove(requestId);
		if (acceptor != null) {
			return Optional.of(acceptor.getDelegate());
		} else {
			return Optional.absent();
		}
	}

	/**
	 * stop tracking of any currently tracked request.
	 */
	@Override
	public void clear() {
		handlers.clear();
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
