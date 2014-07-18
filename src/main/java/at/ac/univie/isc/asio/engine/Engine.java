package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.protocol.Parameters;

import java.security.Principal;

/**
 * An engine capable of creating invocations on a dataset for a specific
 * {@link at.ac.univie.isc.asio.Language}.
 */
public interface Engine {

  /**
   * @return the {@link at.ac.univie.isc.asio.Language query language} supported by this engine.
   */
  Language language();

  /**
   * Create a new, single-use invocation.
   * @param parameters description of operation to perform
   * @param owner initiating user
   * @return ready to be executed Invocation.
   */
  Invocation create(Parameters parameters, Principal owner);
}
