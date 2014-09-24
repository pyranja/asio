package at.ac.univie.isc.asio.admin;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PushObservedEventsTest {
  private PushObservedEvents subject;
  private ByteArrayOutputStream sink;

  @Before
  public void setUp() throws Exception {
    sink = new ByteArrayOutputStream();
    subject = PushObservedEvents.to(ServerSentEvent.Writer.wrap(sink));
  }

  private String output() {
    return new String(sink.toByteArray(), Charsets.UTF_8);
  }

  @Test
  public void writes_event_chunk_in_order() throws Exception {
    subject.onNext(Arrays.asList(
            ServerSentEvent.Simple.create("test", "one"),
            ServerSentEvent.Simple.create("test", "two"))
    );
    assertThat(output(), is("event:test\ndata:one\n\nevent:test\ndata:two\n\n"));
  }

  @Test
  public void write_ping_on_empty_chunk() throws Exception {
    subject.onNext(Collections.<ServerSentEvent>emptyList());
    assertThat(output(), is(":ping\n"));
  }

  @Test
  public void notify_on_completion() throws Exception {
    subject.onCompleted();
    assertThat(output(), is("event:stream\ndata:eof\n\n"));
  }

  @Test
  public void notify_on_error() throws Exception {
    final IllegalStateException cause = new IllegalStateException("test error");
    subject.onError(cause);
    assertThat(output(), is("event:stream\ndata:test error\n\n"));
  }

}
