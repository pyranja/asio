package at.ac.univie.isc.asio.transport;

import com.google.common.base.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JdkPipeTransferTest {
  @Rule
  public Timeout timeout = new Timeout(1000);

  private final static byte[] PAYLOAD = "HELLO WORLD !".getBytes(Charsets.UTF_8);
  private ExecutorService executorService;

  @Before
  public void setUp() {
    executorService = Executors.newFixedThreadPool(2);
  }

  @After
  public void tearDown() {
    executorService.shutdownNow();
  }

  @Test
  public void should_close_both_pipe_ends_when_released() throws Exception {
    final Transfer subject = new JdkPipeTransfer();
    assertThat(subject.source().isOpen(), is(true));
    assertThat(subject.sink().isOpen(), is(true));
    subject.release();
    assertThat(subject.source().isOpen(), is(false));
    assertThat(subject.sink().isOpen(), is(false));
  }

  @Test
  public void can_read_from_source_when_writing_to_sink() throws Exception {
    final Transfer subject = new JdkPipeTransfer();
    subject.sink().write(ByteBuffer.wrap(PAYLOAD));
    final ByteBuffer received = ByteBuffer.allocate(PAYLOAD.length);
    while (received.hasRemaining()) {
      subject.source().read(received);
    }
    assertThat(PAYLOAD, is(received.array()));
  }

  @Test
  public void releasing_transfer_interrupts_producer_and_consumer() throws Exception {
    final Transfer exchange = new JdkPipeTransfer();
    final Callable<Void> producer = new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        while (true) {
          exchange.sink().write(ByteBuffer.wrap(PAYLOAD));
          if (Thread.interrupted()) {
            throw new InterruptedException("producer interrupted");
          }
        }
      }
    };
    final Callable<Void> consumer = new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        final ByteBuffer buf = ByteBuffer.allocate(PAYLOAD.length);
        while(true) {
          while (buf.hasRemaining()) {
            exchange.source().read(buf);
          }
          assertThat(buf.array(), is(equalTo(PAYLOAD)));
          buf.clear();
          if (Thread.interrupted()) {
            throw new InterruptedException("consumer interrupted");
          }
        }
      }
    };
    final Future<Void> producerTask = executorService.submit(producer);
    final Future<Void> consumerTask = executorService.submit(consumer);
    Thread.sleep(1);  // try to let the tasks work a bit
    exchange.release();
    Thread.sleep(1);
    Exception producerException;
    Exception consumerException;
    try {
      producerTask.get();
      fail("producer did not fail");
    } catch (ExecutionException e) {
      assertThat(e.getCause(), instanceOf(ClosedChannelException.class));
    }
    try {
      consumerTask.get();
      fail("consumer did not fail");
    } catch (ExecutionException e) {
      assertThat(e.getCause(), instanceOf(ClosedChannelException.class));
    }
  }
}
