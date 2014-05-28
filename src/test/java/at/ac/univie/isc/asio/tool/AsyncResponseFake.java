package at.ac.univie.isc.asio.tool;

import com.google.common.base.Objects;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Simulate JAX-RS async response handling, cache response object or collect streamed response.
 *
 * @author pyranja
 */
public class AsyncResponseFake implements AsyncResponse {
  // caches
  private Response response = null;
  private Object entity = null;
  private Throwable error = null;

  // state
  private boolean cancelled = false;
  private boolean suspended = true;

  private TimeoutHandler onTimeout = null;

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("cancelled", cancelled)
        .add("suspended", suspended)
        .toString();
  }

  public Response response() {
    failIfNotCompleted();
    checkCachedAvailable(this.response);
    return response;
  }

  public <T> T entity(final Class<T> clazz) {
    failIfNotCompleted();
    checkCachedAvailable(this.entity);
    return clazz.cast(entity);
  }

  public Throwable error() {
    failIfNotCompleted();
    checkCachedAvailable(this.error);
    return error;
  }

  private void failIfNotCompleted() {
    if (cancelled || suspended) { throw new AssertionError("no response : " + this); }
  }

  private void checkCachedAvailable(final Object cached) {
    if (cached == null) { throw new AssertionError("not available"); }
  }

  @Override
  public boolean resume(final Object given) throws IllegalStateException {
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

  public void timeout() {
    if (onTimeout == null) { throw new AssertionError("no handler"); }

  }

  @Override
  public void setTimeoutHandler(final TimeoutHandler handler) {
    onTimeout = handler;
  }

  // ignored
  @Override
  public boolean setTimeout(final long time, final TimeUnit unit) throws IllegalStateException {
    throw new UnsupportedOperationException("fake");
  }

  @Override
  public java.util.Collection<Class<?>> register(final Class<?> callback) throws NullPointerException {
    throw new UnsupportedOperationException("fake");
  }

  @Override
  public java.util.Map<Class<?>, java.util.Collection<Class<?>>> register(final Class<?> callback, final Class<?>... callbacks) throws NullPointerException {
    throw new UnsupportedOperationException("fake");
  }

  @Override
  public java.util.Collection<Class<?>> register(final Object callback) throws NullPointerException {
    throw new UnsupportedOperationException("fake");
  }

  @Override
  public java.util.Map<Class<?>, java.util.Collection<Class<?>>> register(final Object callback, final Object... callbacks) throws NullPointerException {
    throw new UnsupportedOperationException("fake");
  }
}
