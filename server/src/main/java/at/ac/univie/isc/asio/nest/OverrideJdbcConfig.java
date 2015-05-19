/*
 * #%L
 * asio server
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
