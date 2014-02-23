package at.ac.univie.isc.asio.jena;

import java.io.OutputStream;

import org.openjena.riot.Lang;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;

class DescribeHandler implements QueryModeHandler<Model> {

  private final Lang language;

  public DescribeHandler(final Lang language) {
    super();
    this.language = language;
  }

  @Override
  public Model apply(final QueryExecution execution) {
    return execution.execDescribe();
  }

  @Override
  public void serialize(final OutputStream sink, final Model data) {
    data.write(sink, language.getName());
  }

}
