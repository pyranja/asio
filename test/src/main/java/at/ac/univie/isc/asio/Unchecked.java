/*
 * #%L
 * asio test
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
