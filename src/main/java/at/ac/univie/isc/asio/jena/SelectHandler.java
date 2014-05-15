package at.ac.univie.isc.asio.jena;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

import java.io.OutputStream;

class SelectHandler implements QueryModeHandler<ResultSet> {

  private final OutputFormatter serializer;

  public SelectHandler(final OutputFormatter serializer) {
    super();
    this.serializer = serializer;
  }

  @Override
  public ResultSet apply(final QueryExecution execution) {
    final ResultSet result = execution.execSelect();
    result.hasNext(); // force fail-fast on backing store
    return result;
  }

  @Override
  public void serialize(final OutputStream sink, final ResultSet data) {
    serializer.format(sink, data);
  }
}
