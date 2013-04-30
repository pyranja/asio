package at.ac.univie.isc.asio.ogsadai;

import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import uk.org.ogsadai.activity.delivery.StreamExchanger;
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

	private static final String ID_QUALIFIER = "asio";

	private final DRER drer;
	private final StreamExchanger exchanger;

	private final Random rng;

	OgsadaiAdapter(final DRER drer, final StreamExchanger exchanger) {
		this.drer = drer;
		this.exchanger = exchanger;
		rng = new Random();
	}

	/**
	 * Create and submit a synchronous OGSADAI request to execute the given
	 * workflow.
	 * 
	 * @param workflow
	 *            to be executed
	 * @return id of the submitted request
	 * @throws DatasetException
	 *             if an error occurs while communicating with OGSADAI
	 */
	public ResourceID executeSynchronous(final Workflow workflow)
			throws DatasetException {
		final ResourceID requestId = new ResourceID(generateId(), "");
		final CandidateRequestDescriptor request = new SimpleCandidateRequestDescriptor(
				requestId, // randomized with qualifier
				null, // no session
				false, // no session
				true, // synchronous
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

	private String generateId() {
		final long lsb = rng.nextLong();
		final long msb = rng.nextLong();
		final UUID id = new UUID(msb, lsb);
		return ID_QUALIFIER + " - " + id.toString();
	}

	/**
	 * Attach the given stream to the OGSADAI context. The returned id can be
	 * used to retrieve the stream from the {@link StreamExchanger} in the
	 * OGSADAI context.
	 * 
	 * @param stream
	 *            to be attached
	 * @return id associated to the attached stream
	 */
	public String register(final OutputStream stream) {
		final String streamId = generateId();
		exchanger.offer(streamId, stream); // uuids should not collide
		return streamId;
	}

}
