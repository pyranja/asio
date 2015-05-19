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
package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.insight.Event;
import at.ac.univie.isc.asio.insight.Sql;
import at.ac.univie.isc.asio.insight.VndError;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mysql.jdbc.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Emit an event for each query sent to the mysql database. The event describes the executed sql
 * command, total time taken and errors or warnings that occurred during execution.
 */
public final class EventfulMysqlInterceptor implements StatementInterceptorV2 {
  private static final Logger log = getLogger(EventfulMysqlInterceptor.class);

  /*
   * this implementation relies on invariants maintained by the mysql driver and the general usage
   * pattern of the mysql, specifically :
   *  - there is a 1:1 relation between connections and interceptors
   *  - execution of a single sql query is performed by a single thread
   *  - a connection must not perform concurrent sql queries
   * Additionally the shared, global Emitter instance is assumed to be written exactly once,
   * sometime during application startup. Changes to the shared instance will most likely not be
   * picked up by existing interceptors.
   */

  // stopwatch is not thread-safe, assumes external sync by mysql driver
  private final Stopwatch time;
  private Emitter events;

  /** used by mysql connector/j driver */
  public EventfulMysqlInterceptor() {
    this(Ticker.systemTicker(), NO_INIT_SENTINEL);
  }

  @VisibleForTesting
  EventfulMysqlInterceptor(final Ticker time, final Emitter events) {
    this.events = events;
    this.time = Stopwatch.createUnstarted(time);
  }

  /**
   * Prepare measuring of execution duration. Ignore all input parameters.
   */
  @Override
  public ResultSetInternalMethods preProcess(final String sql, final Statement interceptedStatement, final Connection connection) throws SQLException {
    time.reset().start();
    return null;
  }

  /**
   * Emit an event describing the executed sql query.
   */
  @Override
  public ResultSetInternalMethods postProcess(final String sql, final Statement interceptedStatement, final ResultSetInternalMethods originalResultSet, final Connection connection, final int warningCount, final boolean noIndexUsed, final boolean noGoodIndexUsed, final SQLException error) throws SQLException {
    final String sqlCommand = findSqlCommand(sql, interceptedStatement);
    if (isNotDriverInitialization(sqlCommand)) {
      final Sql.SqlEvent event = error == null ? Sql.success(sqlCommand) : Sql.failure(sqlCommand, error);
      event.setBadIndex(noGoodIndexUsed);
      event.setNoIndex(noIndexUsed);
      event.setDuration(time.elapsed(TimeUnit.MILLISECONDS));
      events().emit(event);
    }
    return null;
  }

  /** check the global handover variable if necessary */
  @VisibleForTesting
  Emitter events() {
    /* expected is a deferred initialization exactly once during startup - checking against the
     * constant sentinel avoids a volatile read on every query after init has happened
     */
    if (events == NO_INIT_SENTINEL) {
      events = sharedEmitterInstance;
    }
    return events;
  }

  private String findSqlCommand(final String provided, final Statement statement) {
    if (statement instanceof PreparedStatement) {
        try {
          return ((PreparedStatement) statement).asSql();
        } catch (SQLException ignored) {  // never fail in interceptor
          log.warn(Scope.SYSTEM.marker(), "failed to convert prepared statement into sql command", ignored);
          return "unknown - retrieval failed (" + ignored.getMessage() +")";
        }
    } else {
      return Objects.firstNonNull(provided, "unknown");
    }
  }

  private boolean isNotDriverInitialization(final String sql) {
    // the version comment is added by the connector/j to some initialization queries
    return sql == null || !sql.startsWith("/* mysql-connector-java");
  }

  /**
   * Only intercept top level queries, nested ones are ignored.
   */
  @Override
  public boolean executeTopLevelOnly() {
    return true;
  }

  // === obtain event emitter via static handover from spring context

  /** signals that initialization has not yet happened */
  private static final DummyEmitter NO_INIT_SENTINEL = new DummyEmitter();

  static volatile Emitter sharedEmitterInstance = NO_INIT_SENTINEL;

  @Configuration
  static class Wiring {
    @Autowired
    private Emitter emitter;

    @PostConstruct
    public void shareEmitter() {
      log.info(Scope.SYSTEM.marker(), "using shared emitter {}", emitter);
      sharedEmitterInstance = emitter;
    }
    @PreDestroy
    public void resetSharedEmitter() {
      log.info(Scope.SYSTEM.marker(), "resetting shared emitter");
      sharedEmitterInstance = NO_INIT_SENTINEL;
    }
  }

  @Override
  public void init(final Connection conn, final Properties props) throws SQLException {
    log.debug(Scope.SYSTEM.marker(), "initializing sql interceptor on {} emitting to {}", props, sharedEmitterInstance);
  }

  @Override
  public void destroy() {
    log.debug(Scope.SYSTEM.marker(), "destroying sql interceptor");
  }

  static class DummyEmitter implements Emitter {
    @Override
    public Event emit(final Event event) {
      log.warn("no emitter set on interceptor - lost : {}", event);
      return event;
    }

    @Override
    public VndError emit(final Throwable exception) {
      log.warn("no emitter set on interceptor - lost : {}", exception);
      return null;
    }
  }
}
