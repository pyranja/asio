package at.ac.univie.isc.asio.jena;

import java.io.OutputStream;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

class SelectHandler implements QueryModeHandler<ResultSet> {

  private final OutputFormatter serializer;

  public SelectHandler(final OutputFormatter serializer) {
    super();
    this.serializer = serializer;
  }

  @Override
  public ResultSet apply(final QueryExecution execution) {
    return execution.execSelect();
  }

  @Override
  public void serialize(final OutputStream sink, final ResultSet data) {
    serializer.format(sink, data);
  }
}
