package uk.org.ogsadai.activity.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.exception.DAIException;
import uk.org.ogsadai.resource.ResourceID;
import at.ac.univie.isc.asio.MockDaiException;

@RunWith(MockitoJUnitRunner.class)
public class ActivityErrorListenerTest {

	private static final ResourceID REQUEST = new ResourceID("request", "test");

	private ActivityErrorListener subject;
	@Mock RequestListener parent;

	@Before
	public void setUp() {
		subject = new ActivityErrorListener(parent, REQUEST);
	}

	@Test
	public void forward_error_with_correct_request_id() throws Exception {
		final DAIException error = new MockDaiException();
		subject.error(null, error);
		verify(parent).requestErrorEvent(REQUEST, error);
	}

	@Test
	public void ignore_non_error_events() throws Exception {
		subject.completed(null);
		subject.otherEvent(null, null);
		subject.pending(null);
		subject.processing(null);
		subject.terminated(null);
		subject.warning(null, null);
		verifyZeroInteractions(parent);
	}
}
