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
package at.ac.univie.isc.asio.d2rq.pool;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.d2rq.D2rqTools;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.tool.Beans;
import com.hp.hpl.jena.rdf.model.Model;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import org.slf4j.Logger;
import stormpot.Reallocator;
import stormpot.Slot;

import java.sql.Connection;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Create poolable d2rq model.
 */
final class D2rqModelAllocator implements Reallocator<PooledModel> {
  private static final Logger log = getLogger(D2rqModelAllocator.class);

  public static final int VALIDATION_TIMEOUT = 2; // seconds

  private final D2rqConfigModel d2rq;
  private final Jdbc jdbc;

  public D2rqModelAllocator(final D2rqConfigModel d2rq, final Jdbc jdbc) {
    this.d2rq = d2rq;
    this.jdbc = jdbc;
  }

  @Override
  public PooledModel allocate(final Slot slot) throws Exception {
    log.debug(Scope.SYSTEM.marker(), "allocating a new d2rq model");
    final Model model = d2rq.compile(newConnection());
    return new PooledModel(slot, model);
  }

  private ConnectedDB newConnection() {
    final ConnectedDB connection = D2rqTools.createSqlConnection(jdbc.getUrl(),
        jdbc.getUsername(), jdbc.getPassword(), Beans.asProperties(jdbc.getProperties()));
    connection.switchCatalog(jdbc.getSchema());
    return connection;
  }

  @Override
  public void deallocate(final PooledModel poolable) throws Exception {
    log.debug(Scope.SYSTEM.marker(), "disposing a d2rq model");
    poolable.getModel().close();
  }

  @Override
  public PooledModel reallocate(final Slot slot, final PooledModel poolable) throws Exception {
    log.debug(Scope.SYSTEM.marker(), "attempting reuse of d2rq model");
    final Model model = poolable.getModel();
    try {
      if (canBeReused(model)) {
        log.debug(Scope.SYSTEM.marker(), "reusing live d2rq model");
        return new PooledModel(slot, model);
      } else {
        log.debug(Scope.SYSTEM.marker(), "replacing invalid d2rq model with a fresh one");
        deallocate(poolable);
        return allocate(slot);
      }
    } catch (Exception e) {
      log.warn(Scope.SYSTEM.marker(), "failure during reallocation", e);
      deallocate(poolable);
      throw e;
    }
  }

  private boolean canBeReused(final Model model) throws SQLException {
    if (model.isClosed()) {
      return false;
    } else {
      final Connection connection = D2rqTools.unwrapDatabaseConnection(model).connection();
      return connection.isValid(VALIDATION_TIMEOUT);
    }
  }
}
