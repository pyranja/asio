package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.engine.Invocation;

/**
 * Permit or forbid execution of operations.
 */
public interface Authorizer {
  /**
   * Decide whether the given invocation may be executed.
   * @param invocation prepared operation
   * @throws java.lang.RuntimeException if execution is not permitted
   */
  void check(Invocation invocation) throws RuntimeException;
}
