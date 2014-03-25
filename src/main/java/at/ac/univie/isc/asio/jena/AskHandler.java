package at.ac.univie.isc.asio.jena;

import java.io.OutputStream;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

class AskHandler implements QueryModeHandler<Boolean> {

  private final OutputFormatter serializer;

  public AskHandler(final OutputFormatter serializer) {
    super();
    this.serializer = serializer;
  }

  @Override
  public Boolean apply(final QueryExecution execution) {
    return execution.execAsk();
  }

  @Override
  public void serialize(final OutputStream sink, final Boolean data) {
    serializer.format(sink, data);
  }
}
