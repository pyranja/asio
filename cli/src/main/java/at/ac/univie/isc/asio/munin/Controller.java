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

import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Parse command line arguments and invoke requested command.
 * Expect one non-option argument (i.e. no leading hyphens) as command name, maybe followed by
 * additional non-option arguments. Dispatch execution with parsed arguments to requested command.
 */
class Controller {
  private static final Logger log = getLogger(Controller.class);

  private static final String UNKNOWN_COMMAND = "[ERROR] unknown command '%s'%n%s%n";
  private static final String MISSING_COMMAND = "[ERROR] missing command%n%s%n";
  private static final String COMMAND_FAILED = "[ERROR] '%s' failed - %s%n";

  public static final int CODE_SUCCESS = 0;
  public static final int CODE_ERROR = 1;
  public static final int CODE_WRONG_USAGE = 2;

  private final Appendable out;
  private final Appendable err;
  private final Map<String, Command> commands;

  // set during run
  private String commandName;
  private List<String> commandArguments = new ArrayList<>();

  private int exitCode = CODE_ERROR;

  public Controller(final Appendable out, final Appendable err, final Map<String, Command> commands) {
    this.out = out;
    this.err = err;
    this.commands = commands;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void run(final String... args) throws Exception {
    log.debug("parsing command line {}", (Object) args);
    parse(args);
    log.info("dispatching command {} with arguments {}", commandName, commandArguments);
    dispatch(commandName, commandArguments);
    log.info("done");
    out.append(System.lineSeparator());
  }

  /**
   * Extract the positional arguments, the first is interpreted as command name.
   */
  void parse(final String... args) {
    // filter out the positional arguments
    final ArrayList<String> positional = new ArrayList<>(args.length);
    for (String argument : args) {
      if (argument != null && !argument.startsWith("-")) {
        positional.add(argument);
      }
    }
    if (positional.size() > 0) {
      commandName = positional.get(0).toLowerCase(Locale.ENGLISH);
      commandArguments.addAll(positional.subList(1, positional.size()));
    }
  }

  /**
   *  Invoke the command with given name and pass arguments to it.
   */
  void dispatch(final String name, final List<String> arguments) throws Exception {
    if (name == null) {
      err.append(Pretty.format(MISSING_COMMAND, usage()));
      exitCode = CODE_WRONG_USAGE;
    } else if (name.equalsIgnoreCase("help")) {
      out.append(usage());
      exitCode = CODE_SUCCESS;
    } else {
      final Command command = commands.get(name);
      if (command == null) {
        err.append(Pretty.format(UNKNOWN_COMMAND, name, usage()));
        exitCode = CODE_WRONG_USAGE;
      } else {
        try {
          exitCode = command.call(arguments);
        } catch (Exception error) {
          exitCode = CODE_ERROR;
          err.append(Pretty.format(COMMAND_FAILED, name, error.getMessage()));
        }
      }
    }
  }

  /** create usage message */
  String usage() {
    final StringBuilder message = new StringBuilder();
    message.append(Pretty.format("%n### usage: asio <command> <arg1> <arg2>"));
    for (Map.Entry<String, ?> command : commands.entrySet()) {
      message.append(Pretty.format("%n  %-10s : %s", command.getKey(), command.getValue()));
    }
    message.append(Pretty.format("%n  %-10s : this usage message%n", "help"));
    return message.toString();
  }

  // === for testing

  List<String> getCommandArguments() {
    return commandArguments;
  }

  String getCommandName() {
    return commandName;
  }
}
