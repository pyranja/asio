/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamWriter;

import static java.util.Objects.requireNonNull;

/**
 * An action, which should close its input object.
 *
 * @see AutoCloseable
 * @param <T> type of closed object
 */
public abstract class Closer<T> {
  final static Logger log = LoggerFactory.getLogger(Closer.class);
  /**
   * Close the given, non-null object.
   *
   * @param it the object that should be closed
   * @throws Exception any error during closing it
   */
  public abstract void close(@Nonnull T it) throws Exception;

  // === helper to quietly close objects ===========================================================

  private static final String ERROR_MSG = "error while cleaning up {} : {}";

  /**
   * Close the given {@link AutoCloseable resource} if it is not null. If an exception occurs while
   * closing, it is logged with level WARN, but not rethrown.
   *
   * @param that to be closed
   */
  public static void quietly(@Nullable final AutoCloseable that) {
    quietly(that, autoCloseable());
  }

  /**
   * Quietly close the given object, using the given action. Any exception during execution of the
   * closing action is caught and logged, but not rethrown, except if it is an {@link Error}.
   *
   * @param it the object that should be closed
   * @param closer action that will close the instance
   * @param <T> type of closed object
   */
  public static <T> void quietly(@Nullable final T it, @Nonnull final Closer<T> closer) {
    requireNonNull(closer, "missing closer action");
    if (it != null) {
      try {
        closer.close(it);
      } catch (Exception e) {
        log.warn(ERROR_MSG, it, e.getMessage(), e);
      }
    } else {
      log.warn(ERROR_MSG, "<unknown>", "was null");
    }
  }

  public static AutoCloseableCloser autoCloseable() {
    return AUTO_CLOSEABLE_CLOSER;
  }

  public static Closer<XMLStreamWriter> xmlStreamWriter() {
    return XML_STREAM_WRITER_CLOSER;
  }

  private static final AutoCloseableCloser AUTO_CLOSEABLE_CLOSER = new AutoCloseableCloser();

  private static final XmlStreamWriterCloser XML_STREAM_WRITER_CLOSER = new XmlStreamWriterCloser();

  private static class AutoCloseableCloser extends Closer<AutoCloseable> {
    @Override
    public void close(@Nonnull final AutoCloseable it) throws Exception {
      it.close();
    }
  }

  private static class XmlStreamWriterCloser extends Closer<XMLStreamWriter> {
    @Override
    public void close(@Nonnull final XMLStreamWriter it) throws Exception {
      it.close();
    }
  }
}
