package uk.org.ogsadai.activity.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.COMPLETED;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.COMPLETED_WITH_ERROR;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.ERROR;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.PROCESSING;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.PROCESSING_WITH_ERROR;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.TERMINATED;
import static uk.org.ogsadai.resource.request.RequestExecutionStatus.UNSTARTED;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.RequestTerminatedException;
import at.ac.univie.isc.asio.test.MockDaiException;

@RunWith(MockitoJUnitRunner.class)
public class EventAcceptorTest {

	private EventAcceptor subject;
	@Mock private CompletionCallback delegate;

	@Before
	public void setUp() {
		subject = new EventAcceptor(delegate);
	}

	@Test
	public void ignore_non_terminal_states() throws Exception {
		assertFalse(subject.handleStateAndStop(PROCESSING));
		assertFalse(subject.handleStateAndStop(PROCESSING_WITH_ERROR));
		assertFalse(subject.handleStateAndStop(UNSTARTED));
		verifyZeroInteractions(delegate);
	}

	@Test
	public void propagate_request_completion() throws Exception {
		subject.handleStateAndStop(COMPLETED);
		verify(delegate).complete();
	}

	@Test
	public void signal_stop_on_completion() throws Exception {
		assertTrue(subject.handleStateAndStop(COMPLETED));
	}

	@Test
	public void propagate_request_cancellation() throws Exception {
		subject.handleStateAndStop(TERMINATED);
		verify(delegate).fail(any(RequestTerminatedException.class));
	}

	@Test
	public void signal_stop_on_cancellation() throws Exception {
		assertTrue(subject.handleStateAndStop(TERMINATED));
	}

	@Test(expected = AssertionError.class)
	public void fails_fast_on_illegal_status() throws Exception {
		subject.handleStateAndStop(COMPLETED_WITH_ERROR);
	}

	@Test(expected = AssertionError.class)
	public void propagate_fail_on_illegal_status() throws Exception {
		try {
			subject.handleStateAndStop(COMPLETED_WITH_ERROR);
		} finally {
			verify(delegate).fail(any(DAIException.class));
		}
	}

	@Test
	public void propagates_errors() throws Exception {
		final DAIException cause = new MockDaiException();
		subject.handleErrorAndStop(cause);
		verify(delegate).fail(cause);
	}

	@Test
	public void signal_continue_after_error() throws Exception {
		final DAIException cause = new MockDaiException();
		assertFalse(subject.handleErrorAndStop(cause));
	}

	@Test
	public void signal_stop_after_error_if_in_error_state() throws Exception {
		subject.handleStateAndStop(ERROR);
		final DAIException cause = new MockDaiException();
		assertTrue(subject.handleErrorAndStop(cause));
	}

	@Test
	public void signal_continue_on_request_error_state_and_no_previous_exceptions()
			throws Exception {
		assertFalse(subject.handleStateAndStop(ERROR));
	}

	@Test
	public void signal_stop_on_request_error_state_and_previous_exceptions()
			throws Exception {
		subject.handleErrorAndStop(new MockDaiException());
		assertTrue("signaled continue", subject.handleStateAndStop(ERROR));
	}
}
