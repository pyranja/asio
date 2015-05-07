package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.database.Jdbc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;
import java.util.HashMap;

import static java.util.Objects.requireNonNull;

/**
 * If global jdbc connection settings are present, override the config in any deployed container.
 * All properties, except for the name of the backing database schema, are discarded.
 */
@Brood
@Order(Ordered.HIGHEST_PRECEDENCE + 50) // allow other configurers access to original config
@ConditionalOnProperty(AsioFeatures.GLOBAL_DATASOURCE)
final class OverrideJdbcConfig implements Configurer {
  private final Jdbc override;

  /** required for component scanning */
  @Autowired
  OverrideJdbcConfig(final AsioSettings config) {
    this(config.jdbc);
  }

  OverrideJdbcConfig(final Jdbc override) {
    requireNonNull(override, "override jdbc connection config is null!");
    this.override = override;
  }

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    input.getJdbc()
        .setUrl(override.getUrl())
        .setDriver(override.getDriver())
        .setUsername(override.getUsername())
        .setPassword(override.getPassword())
        .setProperties(new HashMap<>(override.getProperties()));
    return input;
  }

  @Override
  public String toString() {
    return "OverrideJdbcConfig{" +
        "override=" + override +
        '}';
  }
}
