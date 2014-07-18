package at.ac.univie.isc.asio.tool;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Invoke methods and wrap checked exceptions as {@code AssertionError}.
 */
public class Unchecked {
  private Unchecked() {
  }

  public static void write(final StreamingOutput streamingOutput, final OutputStream sink) {
    try {
      streamingOutput.write(sink);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static void await(final CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }
}
