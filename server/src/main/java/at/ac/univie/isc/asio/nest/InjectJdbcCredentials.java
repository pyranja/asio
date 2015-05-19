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
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.security.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;

@Brood
@Order(Ordered.LOWEST_PRECEDENCE) // must be ordered after OverrideJdbcConfig
@ConditionalOnProperty(AsioFeatures.MULTI_TENANCY)
final class InjectJdbcCredentials implements Configurer, OnClose {

  private final MysqlUserRepository repository;

  @Autowired
  public InjectJdbcCredentials(final MysqlUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public String toString() {
    return "InjectJdbcCredentials{}";
  }

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Jdbc jdbc = input.getJdbc();
    final Identity credentials = repository.createUserFor(jdbc.getSchema());
    jdbc.setUsername(credentials.getName()).setPassword(credentials.getSecret());
    return input;
  }

  @Override
  public void cleanUp(final NestConfig spec) throws RuntimeException {
    repository.dropUserOf(spec.getJdbc().getSchema());
  }
}
