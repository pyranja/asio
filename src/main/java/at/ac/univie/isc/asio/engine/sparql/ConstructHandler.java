package at.ac.univie.isc.asio.engine.sparql;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

final class ConstructHandler extends JenaQueryHandler.BaseQueryHandler<Model> {

  private final RDFWriter writer;

  public ConstructHandler(final RDFWriter writer, final MediaType format) {
    super(format);
    this.writer = writer;
  }

  @Override
  protected Model doInvoke(final QueryExecution execution) {
    return execution.execConstruct();
  }

  @Override
  protected void doSerialize(final OutputStream sink, final Model data) {
    writer.write(data, sink, "");
  }
}
