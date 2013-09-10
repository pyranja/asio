package at.ac.univie.isc.asio.jena;

import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.io.OutputSupplier;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Execute a given ARQ query on a set model and stream the results to a receiver.
 * 
 * @author Chris Borckholder
 */
public class QueryTask implements Callable<Void> {

  private static final long TIMEOUT_TO_FIRST = 30L;
  private static final long TIMEOUT_OVERALL = 60L;
  private static final TimeUnit UNIT = TimeUnit.SECONDS;

  // asio
  private final OutputSupplier<OutputStream> receiver;
  private final JenaFormats format;
  // ARQ
  private final Query query;
  private final Model model;

  QueryTask(final OutputSupplier<OutputStream> receiver, final JenaFormats format,
      final Query query, final Model model) {
    super();
    this.receiver = receiver;
    this.format = format;
    this.query = query;
    this.model = model;
  }

  @Override
  public Void call() throws Exception {
    QueryExecution exec = null;
    try {
      exec = QueryExecutionFactory.create(query, model);
      exec.setTimeout(TIMEOUT_TO_FIRST, UNIT, TIMEOUT_OVERALL, UNIT);
      final ResultSet result = exec.execSelect();
      try (OutputStream sink = receiver.getOutput()) {
        ResultSetFormatter.output(sink, result, format.asJenaFormat());
      }
    } finally {
      close(exec);
    }
    return null;
  }

  private void close(final QueryExecution toClose) {
    if (toClose != null) {
      toClose.close();
    }
  }
}
