package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("unused")
@Component
final class ReportSettings {
  private static final Logger log = getLogger(ReportSettings.class);

  private final Environment environment;
  private final AsioSettings settings;

  @Autowired
  public ReportSettings(final Environment environment, final AsioSettings settings) {
    this.environment = environment;
    this.settings = settings;
  }

  private static final String MESSAGE_TEMPLATE =
      "%n%n ===== active profiles: %s ===== %n%s%n%n";

  @PostConstruct
  public void report() {
    final String message = Pretty.format(
        MESSAGE_TEMPLATE, Arrays.toString(environment.getActiveProfiles()), settings
    );
    log.info(Scope.SYSTEM.marker(), message);
  }
}
