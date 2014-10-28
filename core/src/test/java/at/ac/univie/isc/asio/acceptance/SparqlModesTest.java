package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.sql.ConvertToTable;
import at.ac.univie.isc.asio.FunctionalTest;
import com.google.common.collect.Table;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjena.riot.Lang;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URI;

import static at.ac.univie.isc.asio.junit.IsIsomorphic.isomorphicWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class SparqlModesTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return readAccess().resolve("sparql");
  }

  @Test
  public void should_execute_sparql_select() throws Exception {
    final String query =
        "SELECT ?id WHERE { <http://test.com/integration/PUBLIC/PERSON/1> <http://test.com/integration/vocab/PUBLIC_PERSON_ID> ?id }";
    response = client().request(Mime.CSV.type()).post(Entity.entity(query, Mime.QUERY_SPARQL.type()));
    final Table<Integer, String, String> result =
        ConvertToTable.fromCsv(response.readEntity(InputStream.class));
    assertThat(result.cellSet(), hasSize(1));
    assertThat(result.get(0, "id"), is("1"));
  }

  @Test
  public void should_execute_sparql_ask() throws Exception {
    final String query = "ASK { ?s rdfs:label ?o }";
    response = client().request(Mime.CSV.type()).post(Entity.entity(query, Mime.QUERY_SPARQL.type()));
    final String result = response.readEntity(String.class);
    assertThat(result, is(equalToIgnoringWhiteSpace("_askResult true")));
    // for jena 2.9.4 : expect "_askResult true"
    // pre jena 2.9.4 : expect "yes"
  }

  @Test
  public void should_execute_sparql_construct() throws Exception {
    final Model expected = ModelFactory.createDefaultModel();
    expected.createResource("http://example.com/human").addProperty(FOAF.name, "test-name");
    final String query =
        "CONSTRUCT { <http://example.com/human> <http://xmlns.com/foaf/0.1/name> 'test-name' } WHERE { }";
    response = client().request(Mime.XML.type()).post(Entity.entity(query, Mime.QUERY_SPARQL.type()));
    final Model constructed = ModelFactory.createDefaultModel();
    constructed.read(response.readEntity(InputStream.class), null, Lang.RDFXML.getName());
    assertThat(constructed, is(isomorphicWith(expected)));
  }

  @Test
  public void should_execute_sparql_describe() throws Exception {
    final String query = "DESCRIBE <http://localhost:2020/PUBLIC_PERSON/1>";
    response = client().request().post(Entity.entity(query, Mime.QUERY_SPARQL.type()));
    /* execution without error is sufficient - format not defined */
    assertThat(response.getStatusInfo().getFamily(), is(Status.Family.SUCCESSFUL));
  }
}
