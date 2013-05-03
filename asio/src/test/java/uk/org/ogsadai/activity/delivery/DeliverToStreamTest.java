package uk.org.ogsadai.activity.delivery;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.org.ogsadai.activity.ActivityUserException;
import uk.org.ogsadai.activity.io.ActivityIOException;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class DeliverToStreamTest {

	private static final byte[] PAYLOAD = "TESTDATA".getBytes(Charsets.UTF_8);
	private static final String STREAM_ID = "stream-id";

	private DeliverToStreamActivity subject;
	@Spy private final InputStream source = new ByteArrayInputStream(PAYLOAD);
	@Spy private final ByteArrayOutputStream sink = new ByteArrayOutputStream();
	@Mock private StreamExchanger exchanger;

	@Before
	public void setUp() {
		subject = new DeliverToStreamActivity(exchanger);
		when(exchanger.take(STREAM_ID)).thenReturn(
				Optional.<OutputStream> of(sink));
	}

	@Test(expected = NullPointerException.class)
	public void null_id_input_fails() throws Exception {
		subject.processIteration(asArgs(null, source));
	}

	@Test(expected = NullPointerException.class)
	public void null_stream_input_fails() throws Exception {
		subject.processIteration(asArgs(STREAM_ID, null));
	}

	@Test
	public void iteration_writes_data_with_cleanup() throws Exception {
		subject.processIteration(asArgs(STREAM_ID, source));
		assertArrayEquals(PAYLOAD, sink.toByteArray());
		verify(source).close();
		verify(sink).close();
	}

	@Test(expected = ActivityUserException.class)
	public void exchanger_has_no_stream() throws Exception {
		when(exchanger.take(STREAM_ID)).thenReturn(
				Optional.<OutputStream> absent());
		try {
			subject.processIteration(asArgs(STREAM_ID, source));
		} finally {
			verify(source).close();
		}
	}

	@Test(expected = ActivityIOException.class)
	public void copying_fails() throws Exception {
		// XXX failure depends on ByteStreams impl !
		final InputStream mockSource = Mockito.mock(InputStream.class);
		when(mockSource.read(Matchers.<byte[]> any())).thenThrow(
				new IOException("test-exception"));
		try {
			subject.processIteration(asArgs(STREAM_ID, mockSource));
		} finally {
			verify(sink).close();
			verify(mockSource).close();
		}
	}

	private Object[] asArgs(final String id, final InputStream stream) {
		return new Object[] { id, stream };
	}
}
