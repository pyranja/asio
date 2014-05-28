package at.ac.univie.isc.asio.frontend;

import com.google.common.primitives.Bytes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import at.ac.univie.isc.asio.DatasetTransportException;
import at.ac.univie.isc.asio.MockDatasetException;
import at.ac.univie.isc.asio.tool.Payload;
import rx.Observable;

import static com.google.common.collect.Iterators.cycle;
import static com.google.common.collect.Iterators.limit;
import static com.google.common.collect.Iterators.toArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author pyranja
 */
public class StreamingOutputFromObservableTest {

  @Rule
  public ExpectedException error = ExpectedException.none();

  private StreamingOutputFromObservable subject;
  private ByteArrayOutputStream sink;
  private Observable<byte[]> input;
  public static final byte[][] RANDOM_CHUNKS =
      toArray(limit(cycle(Payload.randomWithLength(2048)), 42), byte[].class);

  @Before
  public void setup() {
    sink = spy(new ByteArrayOutputStream());
  }

  @Test
  public void should_yield_nothing_on_empty() throws Exception {
    input = Observable.<byte[]>empty();
    consume();
    assertThat(sink.toByteArray(), is(new byte[0]));
  }

  @Test
  public void should_write_observed_data() throws Exception {
    input = Observable.from(RANDOM_CHUNKS);
    consume();
    final byte[] expected = Bytes.concat(RANDOM_CHUNKS);
    assertThat(sink.toByteArray(), is(equalTo(expected)));
  }

  @Test
  public void should_not_close_stream_after_consumption() throws Exception {
    input = Observable.from(RANDOM_CHUNKS);
    consume();
    verify(sink, never()).close();
  }

  @Test
  public void should_propagate_observed_error() throws Exception {
    final RuntimeException failure = new RuntimeException("TEST");
    input = Observable.error(failure);
    error.expect(is(sameInstance(failure)));
    consume();
  }

  @Test
  public void should_propagate_stream_error() throws Exception {
    final IOException failure = new IOException("TEST");
    input = Observable.from(RANDOM_CHUNKS);
    doThrow(failure).when(sink).write(any(byte[].class));
    error.expect(DatasetTransportException.class);
    error.expectCause(is(sameInstance(failure)));
    consume();
  }

  @Test
  public void should_not_close_stream_after_error() throws Exception {
    input = Observable.error(new MockDatasetException());
    error.expect(MockDatasetException.class);
    try {
      consume();
    } finally {
      verify(sink, never()).close();
    }
  }

  private void consume() throws IOException {
    subject = StreamingOutputFromObservable.bridge(input);
    subject.write(sink);
  }
}
