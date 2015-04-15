package at.ac.univie.isc.asio.munin.commands;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.munin.Command;
import at.ac.univie.isc.asio.munin.Pigeon;
import at.ac.univie.isc.asio.tool.Pretty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
final class Deploy implements Command {
  private final Appendable console;
  private final Pigeon pigeon;

  @Autowired
  public Deploy(final Appendable console, final Pigeon pigeon) {
    this.console = console;
    this.pigeon = pigeon;
  }

  @Override
  public String toString() {
    return "deploy or overwrite a container - expects two arguments, the container id and the path to the mapping file";
  }

  @Override
  public int call(final List<String> arguments) throws IOException {
    if (arguments.size() != 2) {
      throw new IllegalArgumentException("must pass 2 arguments - container id and mapping file path");
    }
    final Id target = Id.valueOf(arguments.get(0));
    final byte[] mapping = Files.readAllBytes(Paths.get(arguments.get(1)));
    final Map<String, Object> deployed = pigeon.deploy(target, mapping);
    console.append(Pretty.format("'%s' deployed%n%s%n", target, deployed));
    return 0;
  }
}
