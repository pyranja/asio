package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Pigeon;
import at.ac.univie.isc.asio.ServerStatus;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Joiner;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Determine the server status.
 */
final class Status implements Command {
  private static final Logger log = getLogger(Status.class);

  private static final Joiner COMMA_SEPARATED = Joiner.on(',').skipNulls();

  private static final String STATUS_LINE = "server is %s%n";
  private static final String CONTAINER_LINE = "active containers [%s]%n";

  private final Appendable report;
  private final Pigeon pigeon;

  public Status(final Appendable sink, final Pigeon pigeon) {
    report = sink;
    this.pigeon = pigeon;
  }

  @Override
  public String toString() {
    return "check whether the server is running and show basic information on its state";
  }

  @Override
  public int call(final List<String> arguments) throws IOException{
    ServerStatus status = null;
    try {
      status = pigeon.health();
      assert status != null : "illegal server status";
      report.append(Pretty.format(STATUS_LINE, status));
      if (!ServerStatus.DOWN.equals(status)) {
        final Collection<Id> containers = pigeon.activeContainer();
        if (!containers.isEmpty()) {
          report.append(Pretty.format(CONTAINER_LINE, COMMA_SEPARATED.join(containers)));
        }
      }
    } catch (final Exception error) {
      log.info("error during status check '{}' - got status {}", error.getMessage(), status);
      // status command should always report at least server DOWN
      if (status == null) {
        report.append(Pretty.format(STATUS_LINE, ServerStatus.DOWN));
      }
    }
    return 0;
  }
}
