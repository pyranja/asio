package at.ac.univie.isc.asio.transport;

import at.ac.univie.isc.asio.tool.Payload;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

public class SubscribedOutputStreamTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private TestSubscriber<byte[]> subscriber;
  private SubscribedOutputStream subject;

  @Before
  public void setUp() throws Exception {
    subscriber = new TestSubscriber<byte[]>();
    subject = new SubscribedOutputStream(subscriber);
  }

  @Test
  public void should_be_empty_when_nothing_written() throws Exception {
    subject.close();
    assertThat(subscriber.getOnNextEvents(), is(empty()));
  }

  @Test
  public void should_forward_all_data_written_to_stream() throws Exception {
    final ByteArrayOutputStream produced = new ByteArrayOutputStream();
    final ByteArrayOutputStream consumed = new ByteArrayOutputStream();
    subject = new SubscribedOutputStream(new Subscriber<byte[]>() {
      @Override
      public void onCompleted() {}
      @Override
      public void onError(final Throwable e) {
        throw new AssertionError(e);
      }
      @Override
      public void onNext(final byte[] bytes) {
        consumed.write(bytes, 0, bytes.length);
      }
    });
    for (int i = 0; i < 13; i++) {
      byte[] current = Payload.randomWithLength(3156);
      subject.write(current);
      produced.write(current);
    }
    subject.close();
    assertThat(consumed.toByteArray(), equalTo(produced.toByteArray()));
  }

  @Test
  public void should_forward_partial_chunk_on_flush() throws Exception {
    final byte[] partial = Payload.randomWithLength(596);
    assumeThat(partial.length, is(lessThan(ObservableStream.MAX_CHUNK_SIZE)));
    subject.write(partial);
    subject.flush();
    final byte[] chunk = Iterables.getOnlyElement(subscriber.getOnNextEvents());
    assertThat(chunk, equalTo(partial));
  }

  @Test
  public void should_fail_if_writing_to_closed() throws Exception {
    error.expect(IOException.class);
    subject.close();
    subject.write(Payload.randomWithLength(42));
  }

  @Test
  public void should_not_complete_the_subscriber() throws Exception {
    subject.close();
    assertThat(subscriber.getOnCompletedEvents(), is(empty()));
  }

  @Test
  public void should_fail_if_unsubscribed() throws Exception {
    subscriber.unsubscribe();
    assertThat(subscriber.isUnsubscribed(), is(true));
    error.expect(IOException.class);
    subject.write(Payload.randomWithLength(42));
  }

  @Test
  public void should_not_push_if_buffer_is_empty() throws Exception {
    subject.flush();
    assertThat(subscriber.getOnNextEvents(), is(empty()));
  }
}
