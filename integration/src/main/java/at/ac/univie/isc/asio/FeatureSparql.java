package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.web.HttpCode;
import com.google.common.net.HttpHeaders;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.AsioMatchers.rdfXmlEqualTo;
import static at.ac.univie.isc.asio.web.HttpMatchers.indicates;
import static org.hamcrest.Matchers.is;

/**
 * Extended functionality of SPARQL protocol endpoints.
 * 
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol</a>
 * @see <a href="https://github.com/kasei/sparql11-protocolvalidator">protocol validator</a>
 */
@Category(FunctionalTest.class)
public class FeatureSparql extends IntegrationTest {
  
  // @formatter:off
  
  public class Execute {
    @Test
    public void ask() throws Exception {
      givenPermission("read")
        .param("query", "ASK {}")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .content("sparql.boolean", is("true"));
    }
    
    @Test
    public void select() throws Exception {
      givenPermission("read")
        .param("query", "SELECT (1 AS ?value) {}")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .body("sparql.head.variable.size()", CoreMatchers.is(1))
        .body("sparql.head.variable[0].@name", CoreMatchers.is("value"))
        .body("sparql.results.result.size()", CoreMatchers.is(1))
        .body("sparql.results.result[0].binding.literal", CoreMatchers.is("1"));
    }

    @Test
    public void construct() throws Exception {
      final Model expected = ModelFactory.createDefaultModel();
      expected.createResource("http://example.com/human").addProperty(FOAF.name, "test-name");
      final String query =
          "CONSTRUCT { "
          + "<http://example.com/human> <http://xmlns.com/foaf/0.1/name> 'test-name' "
          + "} WHERE { }";
      givenPermission("read")
        .header(HttpHeaders.ACCEPT, "application/xml")
        .param("query", query)
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .content(is(rdfXmlEqualTo(expected)));
    }

    @Test
    public void describe() throws Exception {
      final String query = "DESCRIBE <http://example.com/human>";
      /* execution without error is sufficient - format not defined */
      givenPermission("read")
        .param("query", query)
      .when()
        .post("/sparql")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL));
    }
  }
  
  public class MediaType {
    @Test
    public void select_for_xml() throws Exception {
      givenPermission("read")
        .param("query", "SELECT (1 AS ?value) {}")
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"));
    }

    @Test
    public void select_for_json() throws Exception {
      givenPermission("read")
        .param("query", "SELECT (1 AS ?value) {}")
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+json"));
    }

    @Test
    public void select_for_csv() throws Exception {
      givenPermission("read")
        .param("query", "SELECT (1 AS ?value) {}")
        .header(HttpHeaders.ACCEPT, "text/csv")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("text/csv"));
    }

    @Test
    public void ask_for_xml() throws Exception {
      givenPermission("read")
        .param("query", "ASK {}")
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"));
    }

    @Test
    public void ask_for_json() throws Exception {
      givenPermission("read")
        .param("query", "ASK {}")
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+json"));
    }

    @Test
    public void describe_for_xml() throws Exception {
      givenPermission("read")
        .param("query", "DESCRIBE <http://example.org>")
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/rdf+xml"));
    }

    @Test
    public void describe_for_json() throws Exception {
      givenPermission("read")
        .param("query", "DESCRIBE <http://example.org>")
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/rdf+json"));
    }

    @Test
    public void describe_for_turtle() throws Exception {
      givenPermission("read")
        .param("query", "DESCRIBE <http://example.org>")
        .header(HttpHeaders.ACCEPT, "text/turtle")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("text/turtle"));
    }

    @Test
    public void construct_for_xml() throws Exception {
      givenPermission("read")
        .param("query", "CONSTRUCT { <s> <p> 1 } WHERE {}")
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/rdf+xml"));
    }

    @Test
    public void construct_for_json() throws Exception {
      givenPermission("read")
        .param("query", "CONSTRUCT { <s> <p> 1 } WHERE {}")
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/rdf+json"));
    }

    @Test
    public void construct_for_turtle() throws Exception {
      givenPermission("read")
        .param("query", "CONSTRUCT { <s> <p> 1 } WHERE {}")
        .header(HttpHeaders.ACCEPT, "text/turtle")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("text/turtle"));
    }
  }
  
  @Ignore("XXX default-graph-uri param not supported")
  public class DefaultGraph {
    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_default_graph_get() throws Exception {
      givenPermission("read")
        .queryParam("query", "ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o }")
        .queryParam("default-graph-uri","http://kasei.us/2009/09/sparql/data/data1.rdf")
      .when()
        .get("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_default_graph_post() throws Exception {
      givenPermission("read")
        .formParam("query", "ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o }")
        .queryParam("default-graph-uri","http://kasei.us/2009/09/sparql/data/data1.rdf")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_default_multi_graph_get() throws Exception {
      givenPermission("read")
        .queryParam("query","ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o . <http://kasei.us/2009/09/sparql/data/data2.rdf> ?p ?o }")
        .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .get("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_default_multi_graph_post() throws Exception {
      givenPermission("read")
        .formParam("query","ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o . <http://kasei.us/2009/09/sparql/data/data2.rdf> ?p ?o }")
        .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_named_multi_graph_get() throws Exception {
      givenPermission("read")
        .queryParam("query", "ASK { GRAPH ?g { ?s ?p ?o } }")
        .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .get("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_named_multi_graph_post() throws Exception {
      givenPermission("read")
        .formParam("query", "ASK { GRAPH ?g { ?s ?p ?o } }")
        .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_mixed_multi_graph_get() throws Exception {
      givenPermission("read")
        .queryParam("query", "ASK { ?x ?y ?z GRAPH ?g { ?s ?p ?o } }")
        .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data3.rdf")
        .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .get("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX default-graph-uri param not supported")
    public void dataset_mixed_multi_graph_post() throws Exception {
      givenPermission("read")
        .formParam("query", "ASK { ?x ?y ?z GRAPH ?g { ?s ?p ?o } }")
        .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data3.rdf")
        .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }

    @Test
    @Ignore("XXX ? FROM not supported in jena ?")
    public void multiple_dataset() throws Exception {
      givenPermission("read")
        .formParam("query", "ASK FROM <http://kasei.us/2009/09/sparql/data/data1.rdf> { <data1.rdf> ?p ?o }")
        .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data2.rdf")
      .when()
        .post("/sparql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(CoreMatchers.is("application/sparql-results+xml"))
        .body("sparql.boolean", CoreMatchers.is("true"));
    }
  }
}
