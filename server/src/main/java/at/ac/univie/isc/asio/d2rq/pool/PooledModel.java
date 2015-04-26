package at.ac.univie.isc.asio.d2rq.pool;

import at.ac.univie.isc.asio.Scope;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import stormpot.Poolable;
import stormpot.Slot;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Combine a d2rq jena dataset with a pool slot and act as a factory of pool-aware query executions.
 * The expected usage pattern is to obtain a PooledModel from the object pool and use it to create a
 * query execution proxy. The proxy will release the PooledModel on close.
 */
final class PooledModel implements Poolable {
  private static final Logger log = getLogger(PooledModel.class);

  private final Slot pool;
  private final Model model;
  // eagerly cached
  private final Dataset dataset;

  public PooledModel(final Slot pool, final Model model) {
    this.pool = pool;
    this.model = model;
    this.dataset = DatasetFactory.create(model);
  }

  /** for reallocation */
  public Model getModel() {
    return model;
  }

  /** release this model back into the pool - use QueryExecution.close() instead */
  @Override
  public void release() {
    pool.release(this);
  }

  /** create an executable query, tied to this model. closing the execution will release this. */
  public QueryExecution execution(final Query query) {
    log.debug(Scope.REQUEST.marker(), "creating query execution with claimed model");
    final QueryExecution original = QueryExecutionFactory.create(query, dataset);
    return Reflection.newProxy(QueryExecution.class, new PoolAwareProxy(original, this));
  }

  private static final class PoolAwareProxy extends AbstractInvocationHandler {
    private final QueryExecution delegate;
    private final Poolable origin;
    private final AtomicBoolean released = new AtomicBoolean(false);

    private PoolAwareProxy(final QueryExecution delegate, final Poolable origin) {
      this.delegate = delegate;
      this.origin = origin;
    }

    @Override
    protected Object handleInvocation(@Nonnull final Object proxy,
                                      @Nonnull final Method method,
                                      @Nonnull final Object[] args)
        throws Throwable {
      try {
        return method.invoke(delegate, args);
      } finally {
        if ("close".equals(method.getName()) && released.compareAndSet(false, true)) {
          log.debug(Scope.REQUEST.marker(), "releasing d2rq model");
          origin.release();
        }
      }
    }

    @Override
    public String toString() {
      return "PoolAwareProxy";
    }
  }
}
