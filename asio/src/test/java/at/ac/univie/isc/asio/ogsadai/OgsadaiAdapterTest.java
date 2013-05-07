package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.delivery.StreamExchanger;
import uk.org.ogsadai.activity.event.RequestEventRouter;
import uk.org.ogsadai.activity.request.status.SimpleRequestStatus;
import uk.org.ogsadai.activity.workflow.Workflow;
import uk.org.ogsadai.exception.RequestProcessingException;
import uk.org.ogsadai.exception.RequestUserException;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.drer.DRER;
import uk.org.ogsadai.resource.drer.SimpleExecutionResult;
import uk.org.ogsadai.resource.request.CandidateRequestDescriptor;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetOperationTracker;
import at.ac.univie.isc.asio.DatasetUsageException;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiAdapterTest {

	private OgsadaiAdapter subject;
	@Mock private DRER drer;
	@Mock private Workflow workflow;
	@Mock private StreamExchanger exchanger;
	@Mock private RequestEventRouter router;
	@Mock private DatasetOperationTracker tracker;

	@Before
	public void setUp() throws Exception {
		subject = new OgsadaiAdapter(drer, exchanger, router);
	}

	@Test
	public void attaches_stream_to_exchanger() throws Exception {
		final OutputStream stream = new ByteArrayOutputStream();
		final String streamId = subject.register(stream);
		verify(exchanger).offer(streamId, stream);
	}

	@Test
	public void tracks_submitted_request() throws Exception {
		final ResourceID requestId = new ResourceID("test-request");
		final SimpleRequestStatus status = new SimpleRequestStatus(requestId);
		status.setRequestExecutionStatus(RequestExecutionStatus.COMPLETED);
		final SimpleExecutionResult result = new SimpleExecutionResult(
				requestId, null, status);
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenReturn(
				result);
		subject.executeSynchronous(workflow, tracker);
		verify(router).track(any(ResourceID.class), same(tracker));
	}

	@Test
	public void delegates_valid_request() throws Exception {
		final ResourceID requestId = new ResourceID("test-request");
		final SimpleRequestStatus status = new SimpleRequestStatus(requestId);
		status.setRequestExecutionStatus(RequestExecutionStatus.COMPLETED);
		final SimpleExecutionResult result = new SimpleExecutionResult(
				requestId, null, status);
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenReturn(
				result);
		final ResourceID receivedId = subject.executeSynchronous(workflow,
				tracker);
		final ArgumentCaptor<CandidateRequestDescriptor> request = ArgumentCaptor
				.forClass(CandidateRequestDescriptor.class);
		verify(drer).execute(request.capture());
		assertSame(workflow, request.getValue().getWorkflow());
		assertEquals(requestId, receivedId);
	}

	@Test(expected = DatasetUsageException.class)
	public void translates_dai_user_exception() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenThrow(
				new RequestUserException());
		subject.executeSynchronous(workflow, tracker);
	}

	@Test(expected = DatasetFailureException.class)
	public void translates_dai_processing_exception() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenThrow(
				new RequestProcessingException());
		subject.executeSynchronous(workflow, tracker);
	}
}
