package at.ac.univie.isc.asio.insight;

/**
 * Enrich simple events and emit them through the event system.
 */
public interface Emitter {
  /**
   * Emit an event containing the given message
   *
   * @param message event description
   */
  void emit(Message message);
}
