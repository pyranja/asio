package at.ac.univie.isc.asio.ogsadai;

import java.util.UUID;

import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.engine.RequestRejectedException;
import uk.org.ogsadai.exception.RequestProcessingException;
import uk.org.ogsadai.exception.RequestTerminatedException;
import uk.org.ogsadai.exception.RequestUserException;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.drer.DRER;
import uk.org.ogsadai.resource.drer.ExecutionResult;
import uk.org.ogsadai.resource.request.CandidateRequestDescriptor;
import uk.org.ogsadai.resource.request.SimpleCandidateRequestDescriptor;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetUsageException;

/**
 * Use an OGSADAI {@link DRER} to create requests and retrieve the status of
 * submitted requests.
 * 
 * @author Chris Borckholder
 */
public class OgsadaiAdapter {

	private static final String REQUEST_QUALIFIER = "asio";

	private final DRER drer;

	OgsadaiAdapter(final DRER drer) {
		this.drer = drer;
	}

	/**
	 * Create an asynchronous OGSADAI request to execute the given workflow.
	 * 
	 * @param workflow
	 *            to be executed
	 * @return id of the submitted request
	 * @throws DatasetException
	 *             if an error occurs while communicating with OGSADAI
	 */
	public ResourceID submit(final Workflow workflow) throws DatasetException {
		final CandidateRequestDescriptor request = new SimpleCandidateRequestDescriptor(
				makeRequestId(), // randomized with qualifier
				null, // no session
				false, // no session
				false, // asynchronous
				false, // no private resources
				workflow);
		try {
			final ExecutionResult result = drer.execute(request);
			return result.getRequestID();
		} catch (final RequestProcessingException | RequestRejectedException
				| RequestTerminatedException e) {
			throw new DatasetFailureException(e);
		} catch (final RequestUserException e) {
			throw new DatasetUsageException(e);
		}
	}

	private ResourceID makeRequestId() {
		final UUID id = UUID.randomUUID();
		return new ResourceID(REQUEST_QUALIFIER, id.toString());
	}

}
