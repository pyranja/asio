package at.ac.univie.isc.asio.jena;

import com.google.common.base.Preconditions;
import com.hp.hpl.jena.query.QueryExecution;

import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

abstract class QueryModeHandler<RESULT> {
  private RESULT result;

  public final void invoke(final QueryExecution execution) {
    requireNonNull(execution);
    Preconditions.checkState(result == null, "query invoked twice");
    this.result = doInvoke(execution);
    assert result != null : "implementation produced null result";
  }

  protected abstract RESULT doInvoke(QueryExecution execution);

  public final void serialize(final OutputStream sink) {
    requireNonNull(sink);
    Preconditions.checkState(result != null, "no query results captured");
    doSerialize(sink, result);
  }

  protected abstract void doSerialize(OutputStream sink, RESULT data);
}
