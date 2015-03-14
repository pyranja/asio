package at.ac.univie.isc.asio;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Invoke methods and wrap checked exceptions as {@code AssertionError}.
 */
public final class Unchecked {
  /** Thrown if recovery from an IO error is unlikely or unnecessary */
  public static class UncheckedIOException extends RuntimeException {
    public UncheckedIOException(final IOException cause) {
      super(cause);
    }
  }

  /**
   * An action that may fail with a checked exception.
   */
  public static interface Action {
    /**
     * Execute the action.
     * @throws Exception if the action fails
     */
    void call() throws Exception;
  }

  private Unchecked() { /* no instances */ }

  public static void run(final Action action) {
    try {
      action.call();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public static void sleep(final long duration, final TimeUnit unit) {
    try {
      Thread.sleep(unit.toMillis(duration));
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
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
