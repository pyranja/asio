package at.ac.univie.isc.asio.munin.commands;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.munin.Command;
import at.ac.univie.isc.asio.munin.Pigeon;
import at.ac.univie.isc.asio.tool.Pretty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
final class Undeploy implements Command {
  private final Appendable console;
  private final Pigeon pigeon;

  @Autowired
  Undeploy(final Appendable console, final Pigeon pigeon) {
    this.console = console;
    this.pigeon = pigeon;
  }

  @Override
  public String toString() {
    return "undeploy the container with given id if present - expects exactly one argument";
  }

  @Override
  public int call(final List<String> arguments) throws IOException {
    if (arguments.size() != 1) {
      throw new IllegalArgumentException("only a single argument allowed - the container id");
    }
    final Id target = Id.valueOf(arguments.get(0));
    final boolean success = pigeon.undeploy(target);
    if (success) {
      console.append(Pretty.format("'%s' undeployed%n", target));
      return 0;
    } else {
      console.append(Pretty.format("no container named '%s' present%n", target));
      return 1;
    }
  }
}
