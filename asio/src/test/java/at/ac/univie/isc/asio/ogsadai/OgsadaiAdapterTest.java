package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.delivery.ObjectExchanger;
import uk.org.ogsadai.activity.event.CompletionCallback;
import uk.org.ogsadai.activity.event.RequestEventRouter;
import uk.org.ogsadai.activity.request.status.AsynchronousRequestStatus;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.exception.RequestProcessingException;
import uk.org.ogsadai.exception.RequestUserException;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.drer.DRER;
import uk.org.ogsadai.resource.drer.ExecutionResult;
import uk.org.ogsadai.resource.drer.SimpleExecutionResult;
import uk.org.ogsadai.resource.request.CandidateRequestDescriptor;
import uk.org.ogsadai.resource.request.RequestStatus;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetUsageException;

import com.google.common.io.OutputSupplier;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiAdapterTest {

	private static final String MOCK_IDENTIFIER = "test-identifier";

	private OgsadaiAdapter subject;
	@Mock private DRER drer;
	@Mock private Workflow workflow;
	@Mock private ObjectExchanger<OutputSupplier<OutputStream>> exchanger;
	@Mock private RequestEventRouter router;
	@Mock private CompletionCallback tracker;
	@Mock private IdGenerator ids;
	@Captor private ArgumentCaptor<CandidateRequestDescriptor> submittedRequest;

	@Before
	public void setUp() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenReturn(
				mock_response());
		when(ids.next()).thenReturn(MOCK_IDENTIFIER);
		subject = new OgsadaiAdapter(drer, exchanger, router, ids);
	}

	@Test
	public void attaches_stream_to_exchanger() throws Exception {
		@SuppressWarnings("unchecked")
		final OutputSupplier<OutputStream> supplier = mock(OutputSupplier.class);
		final String streamId = subject.register(supplier);
		verify(exchanger).offer(streamId, supplier);
	}

	@Test
	public void tracks_submitted_request() throws Exception {
		final ResourceID expectedId = new ResourceID(MOCK_IDENTIFIER);
		subject.invoke(workflow, tracker);
		verify(router).track(eq(expectedId), same(tracker));
	}

	@Test
	public void submits_request_with_given_workflow() throws Exception {
		subject.invoke(workflow, tracker);
		verify(drer).execute(submittedRequest.capture());
		assertSame(workflow, submittedRequest.getValue().getWorkflow());
	}

	@Test
	public void submits_asynchronous_request() throws Exception {
		subject.invoke(workflow, tracker);
		verify(drer).execute(submittedRequest.capture());
		assertFalse(submittedRequest.getValue().isSynchronous());
	}

	@Test
	public void returns_id_of_created_request() throws Exception {
		final ResourceID expected = new ResourceID(MOCK_IDENTIFIER);
		final ResourceID returned = subject.invoke(workflow, tracker);
		assertEquals(expected, returned);
	}

	@Test(expected = DatasetUsageException.class)
	public void translates_dai_user_exception() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenThrow(
				new RequestUserException());
		subject.invoke(workflow, tracker);
	}

	@Test(expected = DatasetFailureException.class)
	public void translates_dai_processing_exception() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenThrow(
				new RequestProcessingException());
		subject.invoke(workflow, tracker);
	}

	// should resemble an async execution result
	private ExecutionResult mock_response() {
		final ResourceID requestId = new ResourceID(MOCK_IDENTIFIER);
		final RequestStatus status = new AsynchronousRequestStatus(requestId);
		return new SimpleExecutionResult(requestId, null, status);
	}
}
