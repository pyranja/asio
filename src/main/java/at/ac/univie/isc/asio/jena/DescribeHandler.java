package at.ac.univie.isc.asio.jena;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import org.openjena.riot.Lang;

import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

final class DescribeHandler extends JenaQueryHandler.BaseQueryHandler<Model> {

  private final Lang language;

  public DescribeHandler(final Lang language) {
    super(MediaType.valueOf(language.getContentType()));
    this.language = language;
  }

  @Override
  protected Model doInvoke(final QueryExecution execution) {
    return execution.execDescribe();
  }

  @Override
  protected void doSerialize(final OutputStream sink, final Model data) {
    data.write(sink, language.getName());
  }

}
