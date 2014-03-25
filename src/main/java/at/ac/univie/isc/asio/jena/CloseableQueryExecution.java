package at.ac.univie.isc.asio.jena;

import com.google.common.base.Supplier;
import com.hp.hpl.jena.query.QueryExecution;

public class CloseableQueryExecution implements AutoCloseable, Supplier<QueryExecution> {
  private final QueryExecution delegate;

  public CloseableQueryExecution(final QueryExecution delegate) {
    super();
    this.delegate = delegate;
  }

  @Override
  public void close() throws Exception {
    delegate.close();
  }

  @Override
  public QueryExecution get() {
    return delegate;
  }
}
