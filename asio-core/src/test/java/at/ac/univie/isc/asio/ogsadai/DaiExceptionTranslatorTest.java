package at.ac.univie.isc.asio.ogsadai;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import uk.org.ogsadai.activity.ActivityProcessingException;
import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.exception.ErrorID;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.MockDatasetException;

public class DaiExceptionTranslatorTest {

	private DaiExceptionTranslator subject;
	private DatasetException translated;

	@Before
	public void setUp() {
		subject = new DaiExceptionTranslator();
	}

	@Test
	public void wraps_unknown_as_failure() throws Exception {
		final Exception error = new IllegalStateException();
		translated = subject.translate(error);
		assertThat((Exception) translated.getCause(), is(error));
		assertThat(translated, instanceOf(DatasetFailureException.class));
	}

	@Test
	public void forwards_dataset_exception_without_wrapping() throws Exception {
		final Exception error = new MockDatasetException();
		translated = subject.translate(error);
		assertThat(translated, is(error));
	}

	@Test
	public void uses_root_cause_as_message_but_preserves_stacktrace()
			throws Exception {
		final Exception error = new ActivityProcessingException(
				new IllegalStateException("my error"));
		final DatasetFailureException expected = new DatasetFailureException(
				new IllegalStateException("my error"));
		translated = subject.translate(error);
		assertThat((Exception) translated.getCause(), is(error));
		assertThat(translated.getMessage(), is(expected.getMessage()));
	}

	@Test
	public void wraps_user_exceptions_as_usage_exception() throws Exception {
		final Exception error = new ActivityUserException(
				ErrorID.ACTIVITY_HAS_WRONG_NUMBER_OF_INPUTS);
		translated = subject.translate(error);
		assertThat(translated, instanceOf(DatasetUsageException.class));
		assertThat((Exception) translated.getCause(), is(error));
	}
}
