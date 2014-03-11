package at.ac.univie.isc.asio.transport;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.google.common.base.Charsets;

public class JdkPipeTransferTest {

  private final static byte[] PAYLOAD = "HELLO WORLD !".getBytes(Charsets.UTF_8);

  @Test
  public void should_close_both_pipe_ends_when_released() throws Exception {
    final Transfer subject = new JdkPipeTransfer();
    assertThat(subject.source().isOpen(), is(true));
    assertThat(subject.sink().isOpen(), is(true));
    subject.release();
    assertThat(subject.source().isOpen(), is(false));
    assertThat(subject.sink().isOpen(), is(false));
  }

  @Test(timeout = 1000)
  public void can_read_from_source_when_writing_to_sink() throws Exception {
    final Transfer subject = new JdkPipeTransfer();
    subject.sink().write(ByteBuffer.wrap(PAYLOAD));
    final ByteBuffer received = ByteBuffer.allocate(PAYLOAD.length);
    while (received.hasRemaining()) {
      subject.source().read(received);
    }
    assertThat(PAYLOAD, is(received.array()));
  }
}
