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

  // stopwatch is not thread-safe, assumes external sync by mysql driver
  private final Stopwatch time;

  /** used by mysql connector/j driver */
  public EventfulMysqlInterceptor() {
    this(Ticker.systemTicker());
  }

  @VisibleForTesting
  EventfulMysqlInterceptor(final Ticker time) {
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
      final Emitter events = sharedEmitterInstance;
      final Sql.SqlEvent event = error == null ? Sql.success(sqlCommand) : Sql.failure(sqlCommand, error);
      event.setBadIndex(noGoodIndexUsed);
      event.setNoIndex(noIndexUsed);
      event.setDuration(time.elapsed(TimeUnit.MILLISECONDS));
      events.emit(event);
    }
    return null;
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

  static volatile Emitter sharedEmitterInstance = new DummyEmitter();

  @Configuration
  static class MysqlInterceptorWiring {
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
      sharedEmitterInstance = new DummyEmitter();
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
