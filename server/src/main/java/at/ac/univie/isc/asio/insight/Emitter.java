package at.ac.univie.isc.asio.insight;

/**
 * Enrich simple events and emit them through the event system.
 */
public interface Emitter {

  /**
   * Emit the given, context-free event. The event <strong>must not</strong> be
   * {@link Event#init(Correlation, long) initialized} yet.
   *
   * @param event an uninitialized event
   * @return the initialized event as it has been published
   */
  Event emit(Event event);

  /**
   * Emit an event describing the given exception.
   *
   * @param exception the exception that occurred
   * @return the error event as it has been published
   */
  VndError emit(Throwable exception);
}
