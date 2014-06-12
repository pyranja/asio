package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.protocol.Parameters;

import java.security.Principal;

/**
 * @author pyranja
 */
public interface Connector {
  Command createCommand(final Parameters params, Principal owner);
}
