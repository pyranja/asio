package at.ac.univie.isc.asio.nest;

/**
 * An action, which has to be run on container destruction.
 */
public interface OnClose {
  /**
   * Perform any necessary clean up task.
   *
   * @param spec describes the destroyed container
   * @throws RuntimeException may throw unchecked exceptions an an error
   */
  void cleanUp(final NestConfig spec) throws RuntimeException;
}
