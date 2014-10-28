package at.ac.univie.isc.asio.engine.sparql;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

final class AskInvocation extends SparqlInvocation<Boolean> {

  private final OutputFormatter serializer;

  public AskInvocation(final OutputFormatter serializer, final MediaType format) {
    super(format);
    this.serializer = serializer;
  }

  @Override
  protected Boolean doInvoke(final QueryExecution execution) {
    return execution.execAsk();
  }

  @Override
  protected void doSerialize(final OutputStream sink, final Boolean data) {
    serializer.format(sink, data);
  }
}
