package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.tool.JdbcTools;
import com.google.common.base.Optional;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * If the jdbc driver name is not configured, attempt to infer it from the jdbc url.
 * Fails immediately if the driver name cannot be inferred.
 */
@Component
final class InferJdbcDriver implements Configurer {
  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Jdbc jdbc = input.getJdbc();
    if (jdbc.getDriver() == null) {
      final String url = jdbc.getUrl();
      final Optional<String> driver = JdbcTools.inferDriverClass(url);
      if (driver.isPresent()) {
        jdbc.setDriver(driver.get());
      } else {
        throw new IllegalArgumentException("no jdbc driver found for <" + url + ">");
      }
    }
    return input;
  }

  @Override
  public String toString() {
    return "InferJdbcDriver{}";
  }
}
