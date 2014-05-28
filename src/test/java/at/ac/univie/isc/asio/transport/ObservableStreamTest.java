package at.ac.univie.isc.asio.transport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import at.ac.univie.isc.asio.tool.Payload;
import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

public class ObservableStreamTest {
  @Rule
  public Timeout timeout = new Timeout(1000);
  @Rule
  public ExpectedException error = ExpectedException.none();

  private TestSubscriber<byte[]> subscriber;

  @Before
  public void setUp() throws Exception {
    subscriber = new TestSubscriber<byte[]>();
  }

  @Test
  public void should_be_empty_when_reading_empty_source() throws Exception {
    final InputStream source = new ByteArrayInputStream(new byte[0]);
    final ObservableStream stream = ObservableStream.from(source);
    assertThat(valueOf(stream.isEmpty()), is(true));
  }

  @Test
  public void should_emit_a_single_buffer_from_small_source() throws Exception {
    final byte[] payload = Payload.randomWithLength(100);
    final InputStream source = new ByteArrayInputStream(payload);
    assertThat(valueOf(ObservableStream.from(source)), is(payload));
  }

  @Test
  public void should_close_source_after_consumption() throws Exception {
    final InputStream source = spy(new ByteArrayInputStream(new byte[0]));
    ObservableStream.from(source).count().toBlocking().single();
    verify(source, atLeastOnce()).close();
  }

  @Test
  public void should_split_stream_in_chunks_if_buffer_size_exceeded() throws Exception {
    final InputStream source =
        new ByteArrayInputStream(Payload.randomWithLength(
            ObservableStream.MAX_CHUNK_SIZE * 2 + 10));
    final int chunk_count =
        ObservableStream.from(source).count().toBlocking().single();
    assertThat(chunk_count, is(3));
  }

  @Test
  public void should_emit_chunks_with_limited_size() throws Exception {
    final InputStream source =
        new ByteArrayInputStream(Payload.randomWithLength(
            ObservableStream.MAX_CHUNK_SIZE * 20 + 100));
    final Observable<byte[]> stream =
        ObservableStream.from(source).filter(CHUNKS_GREATER_THAN_BUFFER_SIZE);
    assertThat(valueOf(stream.isEmpty()), is(true));
  }

  private static final Func1<byte[], Boolean> CHUNKS_GREATER_THAN_BUFFER_SIZE =
      new Func1<byte[], Boolean>() {
        @Override
        public Boolean call(final byte[] bytes) {
          return bytes.length > ObservableStream.MAX_CHUNK_SIZE;
        }
      };

  @Test
  public void should_preserve_transferred_payload() throws Exception {
    final byte[] payload = Payload.randomWithLength(
        ObservableStream.MAX_CHUNK_SIZE * 10 + 500);
    final InputStream source = new ByteArrayInputStream(payload);
    final ByteArrayOutputStream sink =
        ObservableStream.from(source).collect(new ByteArrayOutputStream(), SINK_COLLECTOR)
            .toBlocking().single();
    assertThat(sink.toByteArray(), is(payload));
  }

  private static final Action2<ByteArrayOutputStream, byte[]> SINK_COLLECTOR
      = new Action2<ByteArrayOutputStream, byte[]>() {
    @Override
    public void call(final ByteArrayOutputStream outputStream, final byte[] bytes) {
      outputStream.write(bytes, 0, bytes.length);
    }
  };

  @Test
  public void should_forward_io_error() throws Exception {
    final InputStream source = mock(InputStream.class);
    final IOException failure = new IOException("EXPECTED");
    doThrow(failure).when(source).read(any(byte[].class), anyInt(), anyInt());
    ObservableStream.from(source).unsafeSubscribe(subscriber);
    assertThat(subscriber.getOnErrorEvents(), is(Arrays.<Throwable>asList(failure)));
    subscriber.assertTerminalEvent();
  }

  @Test
  public void should_emit_only_error_event_on_failure() throws Exception {
    final InputStream source = mock(InputStream.class);
    final IOException failure = new IOException("EXPECTED");
    doThrow(failure).when(source).read(any(byte[].class), anyInt(), anyInt());
    ObservableStream.from(source).unsafeSubscribe(subscriber);
    subscriber.assertTerminalEvent();
  }

  private static <T> T valueOf(Observable<T> source) {
    return source.toBlocking().single();
  }
}
