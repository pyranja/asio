package at.ac.univie.isc.asio.jaxrs;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Simulate JAX-RS async response handling, cache response object or collect streamed response.
 */
public final class AsyncResponseFake implements AsyncResponse {
  // caches
  private Response response = null;
  private Object entity = null;
  private Throwable error = null;

  // state
  private boolean cancelled = false;
  private boolean suspended = true;

  // verification
  private final List<Object> callbacks = new ArrayList<>();
  private TimeoutHandler timeoutHandler;
  private int resumeCount = 0;

  private AsyncResponseFake(final boolean startSuspended) {
    this.suspended = startSuspended;
  }

  /** Create fake in initial state */
  public static AsyncResponseFake create() {
    return new AsyncResponseFake(true);
  }

  /** Create fake that has been already resumed */
  public static AsyncResponseFake completed() {
    return new AsyncResponseFake(false);
  }

  /**
   * @return captured response if available
   */
  public Response response() {
    failIfNotCompleted();
    checkCachedAvailable(this.response);
    return response;
  }

  /**
   * @param clazz expected class of entity
   * @param <T> expected type of entity
   * @return captured response entity if available
   */
  public <T> T entity(final Class<T> clazz) {
    failIfNotCompleted();
    checkCachedAvailable(this.entity);
    return clazz.cast(entity);
  }

  /**
   * @return captured error if available
   */
  public Throwable error() {
    failIfNotCompleted();
    checkCachedAvailable(this.error);
    return error;
  }

  /**
   * All callbacks registered on this.
   * @return list of callbacks in registration order
   */
  public List<Object> callbacks() {
    return callbacks;
  }

  /**
   * @return the set timeout handler or null
   */
  public TimeoutHandler timeoutHandler() {
    return timeoutHandler;
  }

  /**
   * @return number of invocations of {@link at.ac.univie.isc.asio.jaxrs.AsyncResponseFake#resume}
   */
  public int timesResumed() {
    return resumeCount;
  }

  private void failIfNotCompleted() {
    if (cancelled || suspended) { throw new AssertionError("no response : " + this); }
  }

  private void checkCachedAvailable(final Object cached) {
    if (cached == null) { throw new AssertionError("not available"); }
  }
  
  // faked implementation

  @Override
  public boolean resume(final Object given) throws IllegalStateException {
    resumeCount++;
    if (notSuspended()) { return false; }
    Object entity = given;
    if (given instanceof Response) {
      // unwrap response
      this.response = (Response) given;
      entity = this.response.getEntity();
    }
    if (entity instanceof StreamingOutput) {
      consumeStreamingResponse((StreamingOutput) entity);
    } else if (entity instanceof Throwable) {
      this.error = (Throwable) entity;
    } else {
      this.entity = entity;
    }
    suspended = false;
    return true;
  }

  private void consumeStreamingResponse(final StreamingOutput callback) {
    try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      callback.write(buffer);
      this.entity = buffer.toByteArray();
    } catch (IOException | WebApplicationException e) {
      error = e;
    }
  }

  @Override
  public boolean resume(final Throwable response) throws IllegalStateException {
    resumeCount++;
    if (notSuspended()) { return false; }
    error = response;
    suspended = false;
    return true;
  }

  private boolean notSuspended() {
    return !suspended;
  }

  @Override
  public boolean cancel() {
    if (notSuspended()) { return false; }
    cancelled = true;
    suspended = false;
    return true;
  }

  @Override
  public boolean cancel(final int retryAfter) {
    return cancel();
  }

  @Override
  public boolean cancel(final Date retryAfter) {
    return cancel();
  }

  @Override
  public boolean isSuspended() {
    return suspended;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean isDone() {
    return !suspended;
  }

  @Override
  public void setTimeoutHandler(final TimeoutHandler handler) {
    assert handler != null : "null as TimeoutHandler registered";
    this.timeoutHandler = handler;
  }

  @Override
  public boolean setTimeout(final long time, final TimeUnit unit) throws IllegalStateException {
    return notSuspended();
  }

  @Override
  public java.util.Collection<Class<?>> register(final Class<?> callback) throws NullPointerException {
    Objects.requireNonNull(callback);
    return Collections.emptyList();
  }

  @Override
  public java.util.Map<Class<?>, java.util.Collection<Class<?>>> register(final Class<?> callback, final Class<?>... callbacks) throws NullPointerException {
    return Collections.emptyMap();
  }

  @Override
  public java.util.Collection<Class<?>> register(final Object callback) throws NullPointerException {
    this.callbacks.add(callback);
    return Collections.emptyList();
  }

  @Override
  public java.util.Map<Class<?>, java.util.Collection<Class<?>>> register(final Object callback, final Object... callbacks) throws NullPointerException {
    this.callbacks.add(callback);
    this.callbacks.addAll(Arrays.asList(callbacks));
    return Collections.emptyMap();
  }
}
