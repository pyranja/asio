package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

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
@Component
class Controller implements CommandLineRunner, ExitCodeGenerator {
  private static final Logger log = getLogger(Controller.class);

  private static final String UNKNOWN_COMMAND = "[ERROR] unknown command '%s'%n%s%n";
  private static final String MISSING_COMMAND = "[ERROR] missing command%n%s%n";
  private static final String COMMAND_FAILED = "[ERROR] '%s' failed - %s%n";

  public static final int CODE_SUCCESS = 0;
  public static final int CODE_ERROR = 1;
  public static final int CODE_WRONG_USAGE = 2;

  private final Appendable sink;
  private final Map<String, Command> commands;

  // set during run
  private String commandName;
  private List<String> commandArguments = new ArrayList<>();

  private int exitCode = CODE_ERROR;

  @Autowired
  public Controller(final Appendable sink, final Map<String, Command> commands) {
    this.sink = sink;
    this.commands = commands;
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }

  @Override
  public void run(final String... args) throws Exception {
    log.debug("parsing command line {}", args);
    parse(args);
    log.info("dispatching command {} with arguments {}", commandName, commandArguments);
    dispatch(commandName, commandArguments);
    log.info("done");
    sink.append(System.lineSeparator());
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
      sink.append(Pretty.format(MISSING_COMMAND, usage()));
      exitCode = CODE_WRONG_USAGE;
    } else if (name.equalsIgnoreCase("help")) {
      sink.append(usage());
      exitCode = CODE_SUCCESS;
    } else {
      final Command command = commands.get(name);
      if (command == null) {
        sink.append(Pretty.format(UNKNOWN_COMMAND, name, usage()));
        exitCode = CODE_WRONG_USAGE;
      } else {
        try {
          exitCode = command.call(arguments);
        } catch (Exception error) {
          exitCode = CODE_ERROR;
          sink.append(Pretty.format(COMMAND_FAILED, name, error.getMessage()));
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
