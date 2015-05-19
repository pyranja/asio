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

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.tool.JdbcTools;
import com.google.common.base.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;

/**
 * If the jdbc driver name is not configured, attempt to infer it from the jdbc url.
 * Fails immediately if the driver name cannot be inferred.
 */
@Brood
@Order(Ordered.LOWEST_PRECEDENCE)
final class InferJdbcDriver implements Configurer {
  /**
   * Thrown if no jdbc driver class is known for a jdbc url.
   */
  public static final class UnknownJdbcDriver extends InvalidUsage {
    protected UnknownJdbcDriver(final String jdbcUrl) {
      super("no jdbc driver found for <" + jdbcUrl + ">");
    }
  }

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
        throw new UnknownJdbcDriver(url);
      }
    }
    return input;
  }

  @Override
  public String toString() {
    return "InferJdbcDriver{}";
  }
}
