package uk.org.ogsadai.activity.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.RequestDescriptor;
import uk.org.ogsadai.activity.pipeline.ActivityPipeline;
import uk.org.ogsadai.activity.request.OGSADAIRequestConfiguration;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.authorization.SecurityContext;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.monitoring.MonitoringFramework;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;

public class LoggingRequestListener implements RequestListener,
		MonitoringFramework {

	/* slf4j-logger */
	final static Logger log = LoggerFactory
			.getLogger(LoggingRequestListener.class);

	@Override
	public void newRequestEvent(final ResourceID requestID,
			final SecurityContext securityContext) {
		log.info("{} - created", requestID);

	}

	@Override
	public void requestWorkflowEvent(final ResourceID requestID,
			final Workflow workflow) {
		// ignore
	}

	@Override
	public void requestPipelineEvent(final ResourceID requestID,
			final ActivityPipeline pipeline) {
		// ignore
	}

	@Override
	public void requestExecutionStatusEvent(final ResourceID requestID,
			final RequestExecutionStatus status) {
		log.info("{} - {}", requestID, status);
	}

	@Override
	public void requestErrorEvent(final ResourceID requestID,
			final DAIException cause) {
		log.error("{} - {}", requestID, cause.getMessage(), cause);
	}

	@Override
	public void registerListeners(final RequestDescriptor request,
			final OGSADAIRequestConfiguration context) {
		final ResourceID requestId = request.getRequestID();
		log.info("{} - attaching activity listener", requestId);
		final ActivityListener logger = new RequestActivityListener(requestId);
		context.registerActivityListener(logger);
	}

	@Override
	public void addActivityListener(final ActivityListener arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addPipeListener(final PipeListener arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeActivityListener(final ActivityListener arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removePipeListener(final PipeListener arg0) {
		throw new UnsupportedOperationException();
	}

}
