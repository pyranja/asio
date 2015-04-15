package at.ac.univie.isc.asio.munin;

import java.io.IOException;
import java.util.List;

/**
 * A single action in the munin client. The class name is the name of the action when called from
 * the command line.
 */
public interface Command {
  /**
   * Execute the action with the given additional arguments.
   *
   * @param arguments ordered command line arguments
   * @return the exit code, non-zero if the action failed
   */
  int call(final List<String> arguments) throws IOException;

  /**
   * Brief description of what the action does. Used in the usage message.
   */
  String toString();
}
