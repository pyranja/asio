package at.ac.univie.isc.asio.jena;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.sparql.resultset.OutputFormatter;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

final class AskHandler extends JenaQueryHandler.BaseQueryHandler<Boolean> {

  private final OutputFormatter serializer;

  public AskHandler(final OutputFormatter serializer) {
    this(serializer, MediaType.WILDCARD_TYPE);
  }

  public AskHandler(final OutputFormatter serializer, final MediaType format) {
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
