package at.ac.univie.isc.asio.ogsadai;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.UNSTARTED;

import javax.annotation.concurrent.ThreadSafe;

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

/**
 * Interact with an in-process OGSADAI instance to execute workflows asynchronously.
 * <p>
 * Use an OGSADAI {@link DRER} to invoke requests. Through an installed {@link RequestEventRouter}
 * the status of submitted requests is tracked.
 * </p>
 * 
 * @author Chris Borckholder
 */
@ThreadSafe
public class OgsadaiAdapter {

  private final DRER drer;
  private final RequestEventRouter router;

  public OgsadaiAdapter(final DRER drer, final RequestEventRouter router) {
    this.drer = drer;
    this.router = router;
  }

  /**
   * Create and submit an asynchronous OGSADAI request to execute the given workflow. Setup request
   * listening to notify the given {@link CompletionCallback}.
   * 
   * @param id of the request
   * @param workflow to be executed
   * @param tracker callback for request termination
   * @return id of the submitted request
   * @throws DatasetException if an error occurs while communicating with OGSADAI
   */
  public void invoke(final String id, final Workflow workflow, final CompletionCallback tracker)
      throws DatasetException {
    final ResourceID requestId = new ResourceID(id, "");
    router.track(requestId, tracker);
    final CandidateRequestDescriptor request = new SimpleCandidateRequestDescriptor(requestId, // randomized
                                                                                               // with
                                                                                               // qualifier
        null, // no session
        false, // no session
        false, // asynchronous
        false, // no private resources
        workflow);
    try {
      final ExecutionResult result = drer.execute(request);
      verifyExecutionResponse(result, requestId);
    } catch (final RequestException | RequestRejectedException | RequestUserException e) {
      router.stopTracking(requestId);
      tracker.fail(e);
    }
  }

  /**
   * Test whether the OGSADAI ExecutionResult satisfies the expectations.
   * 
   * @param response from OGSADAI
   * @param expectedId submitted request id
   * @throws AssertionError if assertions are enabled and the response is malformed
   */
  private void verifyExecutionResponse(final ExecutionResult response, final ResourceID expectedId) {
    // assert response invariances
    final RequestExecutionStatus state = response.getRequestStatus().getExecutionStatus();
    assert state == UNSTARTED : format(ENGLISH, "unexpected execution status %s", state);
    final ResourceID createdRequestId = response.getRequestID();
    assert expectedId.equals(createdRequestId) : format(ENGLISH,
        "id mismatch : submitted [%s] <> created [%s]", expectedId, createdRequestId);
  }
}
