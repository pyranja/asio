/*
 * #%L
 * asio cli
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.Pigeon;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  // === factory

  public static final class Create {
    /** initialize all defined commands */
    public static Map<String, Command> all(final Appendable sink, final Pigeon pigeon) {
      final HashMap<String, Command> commands = new HashMap<>();
      commands.put("status", new Status(sink, pigeon));
      commands.put("trace", new Trace(sink, pigeon));
      commands.put("deploy", new Deploy(sink, pigeon));
      commands.put("undeploy", new Undeploy(sink, pigeon));
      return commands;
    }

    private Create() {}
  }
}
