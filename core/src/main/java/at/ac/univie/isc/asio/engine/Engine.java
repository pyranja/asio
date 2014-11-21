package at.ac.univie.isc.asio.engine;

import java.security.Principal;

/**
 * An engine capable of creating invocations on a dataset for a specific
 * {@link Language}.
 */
public interface Engine {

  /**
   * @return the {@link Language query language} supported by this engine.
   */
  Language language();

  /**
   * Create a new, single-use invocation.
   * @param parameters description of operation to perform
   * @param owner initiating user
   * @return ready to be executed Invocation.
   */
  Invocation prepare(Parameters parameters, Principal owner);
}
