package at.ac.univie.isc.asio.d2rq.pool;

import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.engine.sparql.JenaFactory;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.shared.PrefixMapping;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import stormpot.Config;
import stormpot.LifecycledResizablePool;
import stormpot.QueuePool;
import stormpot.TimeSpreadExpiration;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Maintains an internal pool of d2rq models.
 */
public final class PooledD2rqFactory implements JenaFactory {

  public static JenaFactory using(final D2rqConfigModel d2rq,
                                  final Jdbc jdbc,
                                  final Timeout timeout,
                                  final int size) {
    final Config<PooledModel> config = new Config<>()
        .setAllocator(new D2rqModelAllocator(d2rq, jdbc))
        .setPreciseLeakDetectionEnabled(true)
        .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("d2rq-pool-%d").build())
        .setExpiration(new TimeSpreadExpiration(3, 5, TimeUnit.HOURS))
        .setSize(size)
        ;
    final QueuePool<PooledModel> pool = new QueuePool<>(config);
    return new PooledD2rqFactory(pool, d2rq.getPrefixes(), timeout);
  }

  private final LifecycledResizablePool<PooledModel> pool;
  private final stormpot.Timeout timeout;
  private final PrefixMapping prefixes;
  private long executionTimeout;

  public PooledD2rqFactory(final LifecycledResizablePool<PooledModel> pool,
                           final PrefixMapping prefixes,
                           final Timeout timeout) {
    this.pool = pool;
    this.prefixes = prefixes;
    this.executionTimeout = timeout.getAs(TimeUnit.MILLISECONDS, 0);
    this.timeout = new stormpot.Timeout(executionTimeout, TimeUnit.MILLISECONDS);
  }

  @Override
  public Query parse(final String sparql) {
    final Query query = QueryFactory.create();
    query.getPrefixMapping().withDefaultMappings(prefixes);
    QueryFactory.parse(query, sparql, null, Syntax.syntaxARQ);
    return query;
  }

  @Override
  public QueryExecution execution(final Query query, final Principal owner) {
    try {
      final PooledModel model = pool.claim(timeout);
      if (model == null) {
        throw new QueryTimeoutException("timed out while claiming a d2rq model");
      }
      final QueryExecution execution = model.execution(query);
      execution.setTimeout(executionTimeout, MILLISECONDS);
      return execution;
    } catch (InterruptedException e) {
      throw new CannotAcquireLockException("interrupted while claiming a d2rq model", e);
    }
  }

  @Override
  public void close() {
    pool.shutdown();
  }
}
