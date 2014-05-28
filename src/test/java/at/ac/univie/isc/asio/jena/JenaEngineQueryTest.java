package at.ac.univie.isc.asio.jena;


import static at.ac.univie.isc.asio.tool.IsIsomorphic.isomorphicWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;

import java.nio.channels.Channels;

import org.junit.Before;
import org.junit.Test;
import org.openjena.riot.Lang;

import at.ac.univie.isc.asio.DatasetEngine;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.engine.OperatorCallback;
import at.ac.univie.isc.asio.tool.ByteArrayTransfer;
import at.ac.univie.isc.asio.tool.CsvToTable;

import com.google.common.base.Charsets;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Created with IntelliJ IDEA. User: borck_000 ; Date: 2/23/14 ; Time: 12:01 PM
 */
public class JenaEngineQueryTest {

  private DatasetEngine jena;
  private Model dataset;

  private final OperatorCallback callback = new ExpectSuccessCallback();
  private final ByteArrayTransfer transfer = new ByteArrayTransfer();

  @Before
  public void setUp() throws Exception {
    dataset = ModelFactory.createDefaultModel();
    dataset.createResource("http://example.com/test").addProperty(RDF.value, "test-value");
    final ListeningExecutorService exec = MoreExecutors.sameThreadExecutor();
    jena = new JenaEngine(exec, dataset);
  }

  @Test
  public void should_execute_sparql_select() throws Exception {
    final String query = "SELECT ?val WHERE { [] ?_ ?val }";
    final DatasetOperation op =
        new DatasetOperation("test-op", Action.QUERY, query, JenaFormats.CSV);
    jena.invoke(op, transfer, callback);
    final Table<Integer, String, String> result = CsvToTable.fromChannel(transfer.source());
    assertThat(result.size(), is(1));
    assertThat(result.get(0, "val"), is("test-value"));
  }

  @Test
  public void should_execute_sparql_ask() throws Exception {
    final String query = "ASK { <http://example.com/test> ?_ 'test-value' }";
    final DatasetOperation op =
        new DatasetOperation("test-op", Action.QUERY, query, JenaFormats.CSV);
    jena.invoke(op, transfer, callback);
    final String result = new String(transfer.buffer(), Charsets.UTF_8);
    assertThat(result, is(equalToIgnoringWhiteSpace("yes")));
    // for jena 2.9.4
    // assertThat(result, is("_askResulttrue"));
  }

  @Test
  public void should_execute_sparql_construct() throws Exception {
    final String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
    final DatasetOperation op =
        new DatasetOperation("test-op", Action.QUERY, query, JenaFormats.XML);
    jena.invoke(op, transfer, callback);
    final Model constructed = ModelFactory.createDefaultModel();
    constructed.read(Channels.newInputStream(transfer.source()), null, Lang.RDFXML.getName());
    assertThat(constructed, is(isomorphicWith(dataset)));
  }

  @Test
  public void should_execute_sparql_describe() throws Exception {
    final String query = "DESCRIBE <http://example.com/test>";
    final DatasetOperation op =
        new DatasetOperation("test-op", Action.QUERY, query, JenaFormats.XML);
    jena.invoke(op, transfer, callback);
    // no standardized result format - execution without error is sufficient
  }

  public class ExpectSuccessCallback implements OperatorCallback {
    @Override
    public void completed(final Phase completed) {
      // expected
    }

    @Override
    public void fail(final DatasetException cause) {
      throw new AssertionError(cause);
    }
  }
}
