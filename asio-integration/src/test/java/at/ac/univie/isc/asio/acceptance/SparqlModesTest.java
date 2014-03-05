package at.ac.univie.isc.asio.acceptance;

import static at.ac.univie.isc.asio.tool.IsIsomorphic.isomorphicWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjena.riot.Lang;

import at.ac.univie.isc.asio.tool.CsvToTable;
import at.ac.univie.isc.asio.tool.FunctionalTest;

import com.google.common.base.Charsets;
import com.google.common.collect.Table;
import com.google.common.io.ByteStreams;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

@Category(FunctionalTest.class)
public class SparqlModesTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.READ_ACCESS.resolve("sparql");
  }

  @Test
  public void should_execute_sparql_select() throws Exception {
    final String query =
        "SELECT ?id WHERE { <http://localhost:2020/PUBLIC/PERSON/1> <http://localhost:2020/vocab/PUBLIC_PERSON_ID> ?id }";
    client.accept(CSV).query(PARAM_QUERY, query);
    response = client.get();
    final Table<Integer, String, String> result =
        CsvToTable.fromStream((InputStream) response.getEntity());
    assertThat(result.size(), is(1));
    assertThat(result.get(0, "id"), is("1"));
  }

  @Test
  public void should_execute_sparql_ask() throws Exception {
    final String query = "ASK { ?s rdfs:label ?o }";
    client.accept(CSV).query(PARAM_QUERY, query);
    response = client.get();
    final String result = responseText(response);
    assertThat(result, is(equalToIgnoringWhiteSpace("yes"))); // text format depends on jena version
  }

  private String responseText(final Response response) throws IOException {
    try (InputStream data = (InputStream) response.getEntity()) {
      final byte[] raw = ByteStreams.toByteArray(data);
      return new String(raw, Charsets.UTF_8);
    }
  }

  @Test
  public void should_execute_sparql_construct() throws Exception {
    final Model expected = ModelFactory.createDefaultModel();
    expected.createResource("http://example.com/human").addProperty(FOAF.name, "test-name");
    final String query =
        "CONSTRUCT { <http://example.com/human> <http://xmlns.com/foaf/0.1/name> 'test-name' } WHERE { }";
    client.accept(XML).query(PARAM_QUERY, query);
    response = client.get();
    final Model constructed = ModelFactory.createDefaultModel();
    constructed.read((InputStream) response.getEntity(), null, Lang.RDFXML.getName());
    assertThat(constructed, is(isomorphicWith(expected)));
  }

  @Test
  public void should_execute_sparql_describe() throws Exception {
    final String query = "DESCRIBE <http://localhost:2020/PUBLIC_PERSON/1>";
    client.accept(XML).query(PARAM_QUERY, query);
    response = client.get();
    /* execution without error is sufficient - format not defined */
    assertThat(response.getStatusInfo().getFamily(), is(Status.Family.SUCCESSFUL));
  }
}
