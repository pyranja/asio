package at.ac.univie.isc.asio.jena;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

import java.io.OutputStream;

final class SelectHandler extends QueryModeHandler<ResultSet> {

  private final OutputFormatter serializer;

  public SelectHandler(final OutputFormatter serializer) {
    super();
    this.serializer = serializer;
  }

  @Override
  protected ResultSet doInvoke(final QueryExecution execution) {
    final ResultSet result = execution.execSelect();
    result.hasNext(); // force fail-fast on backing store
    return result;
  }

  @Override
  protected void doSerialize(final OutputStream sink, final ResultSet data) {
    serializer.format(sink, data);
  }
}
