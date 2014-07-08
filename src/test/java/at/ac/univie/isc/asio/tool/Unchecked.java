package at.ac.univie.isc.asio.tool;

import java.util.concurrent.CountDownLatch;

/**
 * Invoke methods and wrap checked exceptions as {@code AssertionError}.
 */
public class Unchecked {
  private Unchecked() {
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
