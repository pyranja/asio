package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Store a spring request context and allow publishing it on another thread.
 */
public final class ContextPropagator implements AutoCloseable {
  private static final Logger log = getLogger(ContextPropagator.class);

  /**
   * Create a propagator, which stores the context of the current thread, as retrieved from
   * {@link RequestContextHolder#currentRequestAttributes()}.
   */
  public static ContextPropagator capture() {
    final Thread thread = Thread.currentThread();
    log.debug(Scope.SYSTEM.marker(), "capturing context of {}", thread);
    return new ContextPropagator(RequestContextHolder.currentRequestAttributes(), thread);
  }

  private final RequestAttributes stored;
  private final Thread origin;

  ContextPropagator(final RequestAttributes attributes, final Thread origin) {
    stored = attributes;
    this.origin = origin;
  }

  /**
   * Set the stored context as the current thread's local context.
   */
  public ContextPropagator publish() {
    final Thread current = Thread.currentThread();
    if (current == origin) {
      log.warn(Scope.SYSTEM.marker(), "publishing context to origin {} again", origin);
    } else {
      log.debug(Scope.SYSTEM.marker(), "publishing context from {} to {}", origin, current);
    }
    RequestContextHolder.setRequestAttributes(stored);
    return this;
  }

  /**
   * Clear the context, which has been previously published to the current thread.
   */
  @Override
  public void close() {
    final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      fail(Pretty.format("cannot clear context of %s - none present", Thread.currentThread()));
    } else if (attributes != stored) {
      fail(Pretty.format("cannot clear context of %s - attributes not published by this propagator (%s)", Thread.currentThread(), this));
    } else {
      log.debug(Scope.SYSTEM.marker(), "clearing context of {}", Thread.currentThread());
      RequestContextHolder.resetRequestAttributes();
    }
  }

  private void fail(final String message) {
    log.error(Scope.SYSTEM.marker(), message);
    throw new IllegalStateException(message);
  }

  @Override
  public String toString() {
    return "ContextPropagator{" +
        ", origin=" + origin +
        '}';
  }

  /**
   * for testing
   */
  RequestAttributes getStoredAttributes() {
    return stored;
  }
}
