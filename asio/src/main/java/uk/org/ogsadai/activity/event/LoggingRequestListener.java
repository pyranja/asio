package uk.org.ogsadai.activity.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.ogsadai.activity.pipeline.ActivityPipeline;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.authorization.SecurityContext;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;

public class LoggingRequestListener implements RequestListener {

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

}
