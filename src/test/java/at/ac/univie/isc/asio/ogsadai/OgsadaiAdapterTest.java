package at.ac.univie.isc.asio.ogsadai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
import at.ac.univie.isc.asio.DatasetUsageException;

@RunWith(MockitoJUnitRunner.class)
public class OgsadaiAdapterTest {

	private OgsadaiAdapter subject;
	@Mock private DRER drer;
	@Mock private Workflow workflow;

	@Before
	public void setUp() throws Exception {
		subject = new OgsadaiAdapter(drer);
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
		final ResourceID receivedId = subject.submit(workflow);
		final ArgumentCaptor<CandidateRequestDescriptor> request = ArgumentCaptor
				.forClass(CandidateRequestDescriptor.class);
		Mockito.verify(drer).execute(request.capture());
		assertSame(workflow, request.getValue().getWorkflow());
		assertEquals(requestId, receivedId);
	}

	@Test(expected = DatasetUsageException.class)
	public void translates_dai_user_exception() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenThrow(
				new RequestUserException());
		subject.submit(workflow);
	}

	@Test(expected = DatasetFailureException.class)
	public void translates_dai_processing_exception() throws Exception {
		when(drer.execute(any(CandidateRequestDescriptor.class))).thenThrow(
				new RequestProcessingException());
		subject.submit(workflow);
	}
}
