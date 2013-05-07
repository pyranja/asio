package uk.org.ogsadai.activity.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.RequestDescriptor;
import uk.org.ogsadai.activity.SimpleRequestDescriptor;
import uk.org.ogsadai.activity.request.OGSADAIRequestConfiguration;
import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.exception.RequestException;
import uk.org.ogsadai.exception.RequestTerminatedException;
import uk.org.ogsadai.resource.ResourceID;
import uk.org.ogsadai.resource.request.RequestExecutionStatus;
import at.ac.univie.isc.asio.DatasetOperationTracker;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class RequestEventRouterTest {

	private static final ResourceID DRER = new ResourceID("drer", "test");
	private static final ResourceID REQUEST = new ResourceID("request", "test");

	private RequestEventRouter subject;
	@Mock private DatasetOperationTracker tracker;
	@Mock private OGSADAIRequestConfiguration context;
	private RequestDescriptor request;

	@Before
	public void setUp() {
		request = new SimpleRequestDescriptor(DRER, REQUEST, null);
		when(context.getRequestID()).thenReturn(REQUEST);
		when(context.getDRER()).thenReturn(DRER);
		// setup subject
		subject = new RequestEventRouter();
	}

	// invariances
	@Test(expected = NullPointerException.class)
	public void fail_on_null_request_id() throws Exception {
		subject.track(null, tracker);
	}

	@Test(expected = NullPointerException.class)
	public void fail_on_null_tracker() throws Exception {
		subject.track(REQUEST, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void fail_on_duplicate_request_id() throws Exception {
		subject.track(REQUEST, tracker);
		subject.track(REQUEST, tracker);
	}

	// behavior
	@Test
	public void propagates_request_completion() throws Exception {
		subject.track(REQUEST, tracker);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.COMPLETED);
		verify(tracker).complete();
	}

	@Test
	public void propagates_request_error() throws Exception {
		subject.track(REQUEST, tracker);
		final DAIException cause = new RequestException(null);
		subject.requestErrorEvent(REQUEST, cause);
		verify(tracker).fail(cause);
	}

	@Test
	public void propagates_request_cancellation() throws Exception {
		subject.track(REQUEST, tracker);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.TERMINATED);
		verify(tracker).fail(any(RequestTerminatedException.class));
	}

	@Test
	public void ignores_intermediate_events() throws Exception {
		subject.track(REQUEST, tracker);
		subject.newRequestEvent(REQUEST, null);
		subject.requestWorkflowEvent(REQUEST, null);
		subject.requestPipelineEvent(REQUEST, null);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.PROCESSING);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.PROCESSING_WITH_ERROR);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.UNSTARTED);
		verifyZeroInteractions(tracker);
	}

	@Test
	public void ignores_untracked_request_events() throws Exception {
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.COMPLETED);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.TERMINATED);
		subject.requestErrorEvent(REQUEST, new RequestException(null));
		verifyZeroInteractions(tracker);
	}

	@Test
	public void does_not_attach_to_non_tracked_request_activities()
			throws Exception {
		subject.registerListeners(request, context);
		verify(context, never()).registerActivityListener(
				any(ActivityListener.class));
	}

	@Test
	public void attaches_activity_listener_to_tracked_requests()
			throws Exception {
		subject.track(REQUEST, tracker);
		subject.registerListeners(request, context);
		verify(context).registerActivityListener(any(ActivityListener.class));
	}

	@Test
	public void does_not_forward_after_stopping_tracking() throws Exception {
		subject.track(REQUEST, tracker);
		subject.requestErrorEvent(REQUEST, new RequestException(null));
		Mockito.verify(tracker).fail(any(Exception.class));
		final Optional<DatasetOperationTracker> removed = subject
				.stopTracking(REQUEST);
		assertTrue(removed.isPresent());
		subject.requestErrorEvent(REQUEST, new RequestException(null));
		Mockito.verifyNoMoreInteractions(tracker);
	}

	@Test
	public void terminal_request_status_stops_tracking() throws Exception {
		subject.track(REQUEST, tracker);
		subject.requestExecutionStatusEvent(REQUEST,
				RequestExecutionStatus.COMPLETED);
		final Optional<DatasetOperationTracker> removed = subject
				.stopTracking(REQUEST);
		assertFalse(removed.isPresent());
	}

	@SuppressWarnings("deprecation")
	@Test(expected = UnsupportedOperationException.class)
	public void unused_monitoring_methods_fail_fast() throws Exception {
		subject.addActivityListener(null);
		subject.addPipeListener(null);
		subject.clear();
		subject.removeActivityListener(null);
		subject.removePipeListener(null);
	}
}
