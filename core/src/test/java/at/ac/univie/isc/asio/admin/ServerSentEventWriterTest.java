package at.ac.univie.isc.asio.admin;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ServerSentEventWriterTest {

  private ServerSentEvent.Writer subject;
  private ByteArrayOutputStream sink;

  @Before
  public void setUp() throws Exception {
    sink = new ByteArrayOutputStream();
    subject = ServerSentEvent.Writer.wrap(sink);
  }

  @Test
  public void write_comment() throws Exception {
    subject.comment("test");
    assertThat(output(), is(":test\n"));
  }

  @Test
  public void write_data() throws Exception {
    subject.data("test");
    assertThat(output(), is("data:test\n"));
  }

  @Test
  public void write_name() throws Exception {
    subject.event("test");
    assertThat(output(), is("event:test\n"));
  }

  @Test
  public void write_id() throws Exception {
    subject.id("test");
    assertThat(output(), is("id:test\n"));
  }

  @Test
  public void write_retry_delay() throws Exception {
    subject.retryAfter(100);
    assertThat(output(), is("retry:100\n"));
  }

  @Test
  public void write_event_separator() throws Exception {
    subject.boundary();
    assertThat(output(), is("\n"));
  }

  @Test
  public void write_two_events() throws Exception {
    subject.event("test-event").id("one").data("test-data").data("test-data-2").boundary();
    subject.data("test-data").id("two").retryAfter(200).boundary();
    final String expected = new StringBuilder()
        .append("event:test-event\nid:one\ndata:test-data\ndata:test-data-2\n\n")
        .append("data:test-data\nid:two\nretry:200\n\n")
        .toString();
    assertThat(output(), is(expected));
  }

  private String output() throws IOException {
    subject.flush();
    return new String(sink.toByteArray(), Charsets.UTF_8);
  }
}
