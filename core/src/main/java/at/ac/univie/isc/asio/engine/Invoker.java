package at.ac.univie.isc.asio.engine;

/**
 * Describe a component that is able to create an {@code Invocation} from a set of
 * {@code Parameters}.
 */
public interface Invoker {
  /**
   * Create a new, single-use invocation.
   * @param parameters description of operation to perform
   * @return ready to be executed Invocation.
   */
  Invocation prepare(Parameters parameters);
}
