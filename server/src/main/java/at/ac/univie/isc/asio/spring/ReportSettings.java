package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("unused")
@Component
final class ReportSettings {
  private static final Logger log = getLogger(ReportSettings.class);

  private final AsioSettings settings;

  @Autowired
  public ReportSettings(final AsioSettings settings) {
    this.settings = settings;
  }

  @PostConstruct
  public void report() {
    final String message =
        Pretty.format("%n=== active settings ===%n%s%n=======================", settings);
    log.info(Scope.SYSTEM.marker(), message);
  }
}
