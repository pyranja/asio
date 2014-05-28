package at.ac.univie.isc.asio.transport;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.MockDatasetException;
import at.ac.univie.isc.asio.tool.Payload;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ObservableStreamBodyWriterTest {
  private static final byte[] PAYLOAD = Payload.randomWithLength(12 * 8 * 1024 + 123);

  @Rule
  public ExpectedException error = ExpectedException.none();

  private ObservableStreamBodyWriter subject;
  private ObservableStream source;
  private ByteArrayOutputStream sink;

  @Before
  public void setup() {
    subject = new ObservableStreamBodyWriter();
    source = ObservableStream.from(new ByteArrayInputStream(PAYLOAD));
    sink = new ByteArrayOutputStream();
  }

  @Test
  public void should_accept_observable_stream() throws Exception {
    final boolean accepted = subject.isWriteable(ObservableStream.class, null, null, null);
    assertThat(accepted, is(true));
  }

  @Test
  public void should_reject_non_observable() throws Exception {
    final boolean accepted = subject.isWriteable(Integer.class, null, null, null);
    assertThat(accepted, is(false));
  }

  @Test
  public void should_reject_null_type() throws Exception {
    final boolean accepted = subject.isWriteable(null, null, null, null);
    assertThat(accepted, is(false));
  }

  @Test
  public void should_report_unknown_length() throws Exception {
    final long size = subject.getSize(null, null, null, null, null);
    assertThat(size, is(-1L));
  }

  @Test
  public void should_write_emitted_chunks_to_output() throws Exception {
    subject.writeTo(source, source.getClass(), null, null, null, null, sink);
    assertThat(sink.toByteArray(), is(equalTo(PAYLOAD)));
  }

  @Test
  public void should_not_close_given_sink() throws Exception {
    sink = spy(sink);
    subject.writeTo(source, source.getClass(), null, null, null, null, sink);
    verify(sink, never()).close();
  }

  @Test
  public void should_not_write_if_source_empty() throws Exception {
    final ObservableStream empty = ObservableStream.from(new ByteArrayInputStream(new byte[0]));
    subject.writeTo(empty, source.getClass(), null, null, null, null, sink);
    assertThat(sink.toByteArray(), is(equalTo(new byte[0])));
  }

  @Test
  public void should_rethrow_source_errors() throws Exception {
    final MockDatasetException failure = new MockDatasetException();
    error.expect(is(failure));
    final ObservableStream failing = ObservableStream.wrap(Observable.<byte[]>error(failure));
    subject.writeTo(failing, source.getClass(), null, null, null, null, sink);
  }

  @Test
  public void should_rethrow_sink_errors() throws Exception {
    sink = spy(sink);
    final IOException failure = new IOException("EXPECTED");
    doThrow(failure).when(sink).write(any(byte[].class));
    error.expect(DatasetException.class);
    error.expectCause(is(failure));
    subject.writeTo(source, source.getClass(), null, null, null, null, sink);
  }

  @Test
  public void should_cancel_streaming_on_sink_error() throws Exception {
    final ByteArrayInputStream source_data = new ByteArrayInputStream(PAYLOAD);
    source = ObservableStream.from(source_data);
    // ensure test precondition
    assertThat(source_data.available(), is(greaterThan(ObservableStream.MAX_CHUNK_SIZE)));
    sink = spy(sink);
    final IOException failure = new IOException("EXPECTED");
    doThrow(failure).when(sink).write(any(byte[].class));
    error.expect(Throwable.class);
    try {
      subject.writeTo(source, source.getClass(), null, null, null, null, sink);
    } finally {
      // as writing the first chunk to sink fails and there is more than one chunk worth of data,
      // the source InputStream should not have been consumed completely
      assertThat(source_data.available(), is(greaterThan(0)));
    }
  }
}
