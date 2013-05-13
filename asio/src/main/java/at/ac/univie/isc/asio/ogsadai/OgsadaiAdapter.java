package at.ac.univie.isc.asio.ogsadai;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.UNSTARTED;

import java.io.OutputStream;

import uk.org.ogsadai.activity.delivery.StreamExchanger;
import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.event.RequestEventRouter;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.engine.RequestRejectedException;
import uk.org.ogsadai.exception.RequestException;
import uk.org.ogsadai.exception.RequestUserException;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.drer.DRER;
import uk.org.ogsadai.resource.drer.ExecutionResult;
import uk.org.ogsadai.resource.request.CandidateRequestDescriptor;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;
import uk.org.ogsadai.resource.request.SimpleCandidateRequestDescriptor;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetUsageException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.OutputSupplier;

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
	private final RequestEventRouter router;
	private final IdGenerator ids;

	@VisibleForTesting
	OgsadaiAdapter(final DRER drer, final StreamExchanger exchanger,
			final RequestEventRouter router, final IdGenerator ids) {
		this.drer = drer;
		this.exchanger = exchanger;
		this.router = router;
		this.ids = ids;
	}

	OgsadaiAdapter(final DRER drer, final StreamExchanger exchanger,
			final RequestEventRouter router) {
		this(drer, exchanger, router, new RandomIdGenerator(ID_QUALIFIER));
	}

	/**
	 * Attach the given stream supplier to the OGSADAI context. The returned id
	 * can be used to retrieve the supplier from the {@link StreamExchanger} in
	 * the OGSADAI context.
	 * 
	 * @param supplier
	 *            to be attached
	 * @return id associated to the attached supplier
	 */
	public String register(final OutputSupplier<OutputStream> supplier) {
		final String supplierId = ids.next();
		exchanger.offer(supplierId, supplier); // should not collide
		return supplierId;
	}

	/**
	 * Invalidate the supplier which may have been registered with the given id.
	 * 
	 * @param supplierId
	 *            of invalid supplier
	 */
	public void revokeSupplier(final String supplierId) {
		exchanger.take(supplierId);
	}

	/**
	 * Create and submit an asynchronous OGSADAI request to execute the given
	 * workflow. Setup request listening to notify the given
	 * {@link CompletionCallback}.
	 * 
	 * @param workflow
	 *            to be executed
	 * @param tracker
	 *            callback for request termination
	 * @return id of the submitted request
	 * @throws DatasetException
	 *             if an error occurs while communicating with OGSADAI
	 */
	public ResourceID invoke(final Workflow workflow,
			final CompletionCallback tracker) throws DatasetException {
		final ResourceID requestId = new ResourceID(ids.next(), "");
		router.track(requestId, tracker);
		final CandidateRequestDescriptor request = new SimpleCandidateRequestDescriptor(
				requestId, // randomized with qualifier
				null, // no session
				false, // no session
				false, // asynchronous
				false, // no private resources
				workflow);
		try {
			final ExecutionResult result = drer.execute(request);
			verifyExecutionResponse(result, requestId);
			return requestId;
			// XXX do not throw here but let callback fail
		} catch (final RequestException | RequestRejectedException e) {
			router.stopTracking(requestId);
			throw new DatasetFailureException(e);
		} catch (final RequestUserException e) {
			router.stopTracking(requestId);
			throw new DatasetUsageException(e);
		}
	}

	/**
	 * Test whether the OGSADAI ExecutionResult satisfies the expectations.
	 * 
	 * @param response
	 *            from OGSADAI
	 * @param expectedId
	 *            submitted request id
	 * @throws AssertionError
	 *             if assertions are enabled and the response is malformed
	 */
	private void verifyExecutionResponse(final ExecutionResult response,
			final ResourceID expectedId) {
		// assert response invariances
		final RequestExecutionStatus state = response.getRequestStatus()
				.getExecutionStatus();
		assert state == UNSTARTED : format(ENGLISH,
				"unexpected execution status %s", state);
		final ResourceID createdRequestId = response.getRequestID();
		assert expectedId.equals(createdRequestId) : format(ENGLISH,
				"id mismatch : submitted [%s] <> created [%s]", expectedId,
				createdRequestId);
	}
}
