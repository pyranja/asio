package at.ac.unvie.isc.asio;

import at.ac.univie.isc.asio.FunctionalTest;
import at.ac.unvie.isc.asio.web.HttpCode;
import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static at.ac.unvie.isc.asio.junit.HttpMatchers.indicates;
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.LogConfig.logConfig;
import static org.hamcrest.Matchers.is;

/**
 * Verify compliance of a HTTP endpoint with the W3C SPARQL Protocol recommendation.
 * <p>
 * All test cases are independent from the endpoint contents.
 * Validation of xml responses is not namespace-aware.
 * </p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL Protocol</a>
 * @see <a href="https://github.com/kasei/sparql11-protocolvalidator">protocol validator</a>
 */
@RunWith(Enclosed.class)
@Category(FunctionalTest.class)
public class SparqlProtocolTest {
  @BeforeClass
  public static void configureRestAssured() {
    baseURI = EnvironmentSpec.current.sparqlEndpoint().toString();
    config = config()
        .encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8.name()))
        .logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails())
    ;
  }

  private static byte[] bytes(final String query) {
    return query.getBytes(Charsets.UTF_8);
  }

  // @formatter:off

  @RunWith(Enclosed.class)
  public static class Query {
    public static class Positive {
      @Test
      public void get() throws Exception {
        given()
            .queryParam("query", "ASK {}")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .body("sparql.boolean", is("true"));
      }

      @Test
      public void post_form() throws Exception {
        given()
            .formParam("query", "ASK {}")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .body("sparql.boolean", is("true"));
      }

      @Test
      public void post_direct() throws Exception {
        given()
            .body(bytes("ASK {}"))
            .contentType("application/sparql-query")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .body("sparql.boolean", is("true"));
      }

      @Test
      public void select_post() throws Exception {
        given()
            .formParam("query", "SELECT (1 AS ?value) {}")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .body("sparql.head.variable.size()", is(1))
            .body("sparql.head.variable[0].@name", is("value"))
            .body("sparql.results.result.size()", is(1))
            .body("sparql.results.result[0].binding.literal", is("1"));
      }

      // XXX : host rdf files locally ?

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_default_graph_get() throws Exception {
        given()
            .queryParam("query", "ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o }")
            .queryParam("default-graph-uri","http://kasei.us/2009/09/sparql/data/data1.rdf")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_default_graph_post() throws Exception {
        given()
            .formParam("query", "ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o }")
            .queryParam("default-graph-uri","http://kasei.us/2009/09/sparql/data/data1.rdf")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_default_multi_graph_get() throws Exception {
        given()
            .queryParam("query","ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o . <http://kasei.us/2009/09/sparql/data/data2.rdf> ?p ?o }")
            .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_default_multi_graph_post() throws Exception {
        given()
            .formParam("query","ASK { <http://kasei.us/2009/09/sparql/data/data1.rdf> ?p ?o . <http://kasei.us/2009/09/sparql/data/data2.rdf> ?p ?o }")
            .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_named_multi_graph_get() throws Exception {
        given()
            .queryParam("query", "ASK { GRAPH ?g { ?s ?p ?o } }")
            .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_named_multi_graph_post() throws Exception {
        given()
            .formParam("query", "ASK { GRAPH ?g { ?s ?p ?o } }")
            .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_mixed_multi_graph_get() throws Exception {
        given()
            .queryParam("query", "ASK { ?x ?y ?z GRAPH ?g { ?s ?p ?o } }")
            .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data3.rdf")
            .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX default-graph-uri param not supported")
      public void dataset_mixed_multi_graph_post() throws Exception {
        given()
            .formParam("query", "ASK { ?x ?y ?z GRAPH ?g { ?s ?p ?o } }")
            .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data3.rdf")
            .queryParam("named-graph-uri", "http://kasei.us/2009/09/sparql/data/data1.rdf", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      @Ignore("XXX ? FROM not supported in jena ?")
      public void multiple_dataset() throws Exception {
        given()
            .formParam("query", "ASK FROM <http://kasei.us/2009/09/sparql/data/data1.rdf> { <data1.rdf> ?p ?o }")
            .queryParam("default-graph-uri", "http://kasei.us/2009/09/sparql/data/data2.rdf")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"))
            .body("sparql.boolean", is("true"));
      }

      @Test
      public void content_type_select_xml() throws Exception {
        given()
            .formParam("query", "SELECT (1 AS ?value) {}")
            .header(HttpHeaders.ACCEPT, "application/xml")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"));
      }

      @Test
      public void content_type_select_json() throws Exception {
        given()
            .formParam("query", "SELECT (1 AS ?value) {}")
            .header(HttpHeaders.ACCEPT, "application/json")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+json"));
      }

      @Test
      public void content_type_select_csv() throws Exception {
        given()
            .formParam("query", "SELECT (1 AS ?value) {}")
            .header(HttpHeaders.ACCEPT, "text/csv")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("text/csv"));
      }

      @Test
      public void content_type_ask_xml() throws Exception {
        given()
            .formParam("query", "ASK {}")
            .header(HttpHeaders.ACCEPT, "application/xml")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+xml"));
      }

      @Test
      public void content_type_ask_json() throws Exception {
        given()
            .formParam("query", "ASK {}")
            .header(HttpHeaders.ACCEPT, "application/json")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/sparql-results+json"));
      }

      @Test
      public void content_type_describe_xml() throws Exception {
        given()
            .formParam("query", "DESCRIBE <http://example.org>")
            .header(HttpHeaders.ACCEPT, "application/xml")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/rdf+xml"));
      }

      @Test
      public void content_type_describe_json() throws Exception {
        given()
            .formParam("query", "DESCRIBE <http://example.org>")
            .header(HttpHeaders.ACCEPT, "application/json")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/rdf+json"));
      }

      @Test
      public void content_type_describe_turtle() throws Exception {
        given()
            .formParam("query", "DESCRIBE <http://example.org>")
            .header(HttpHeaders.ACCEPT, "text/turtle")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("text/turtle"));
      }

      @Test
      public void content_type_construct_xml() throws Exception {
        given()
            .formParam("query", "CONSTRUCT { <s> <p> 1 } WHERE {}")
            .header(HttpHeaders.ACCEPT, "application/xml")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/rdf+xml"));
      }

      @Test
      public void content_type_construct_json() throws Exception {
        given()
            .formParam("query", "CONSTRUCT { <s> <p> 1 } WHERE {}")
            .header(HttpHeaders.ACCEPT, "application/json")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("application/rdf+json"));
      }

      @Test
      public void content_type_construct_turtle() throws Exception {
        given()
            .formParam("query", "CONSTRUCT { <s> <p> 1 } WHERE {}")
            .header(HttpHeaders.ACCEPT, "text/turtle")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.SUCCESSFUL))
            .contentType(is("text/turtle"));
      }
    }


    public static class Negative {
      @Test
      public void method() throws Exception {
        given()
            .queryParam("query", "ASK {}")
        .when()
            .put()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void bad_multiple_queries() throws Exception {
        given()
            .queryParam("query", "ASK {}", "SELECT * {}")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void wrong_media_type() throws Exception {
        given()
            .contentType("text/plain")
            .body("ASK {}")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      @Ignore("cxf ignores missing content-type") // FIXME : strict mode
      public void missing_form_type() throws Exception {
        given() // rest assured cannot serialize without content type
            .body(bytes("query=ASK%20%7B%7D"))
            .header(HttpHeaders.CONTENT_TYPE, "")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void missing_direct_type() throws Exception {
        given()
            .body(bytes("ASK {}"))
            .contentType("")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      @Ignore("cxf handles charset")  // FIXME : strict mode
      public void non_utf8() throws Exception {
        given()
            .body("ASK {}".getBytes(Charsets.UTF_16))
            .contentType("application/sparql-query; charset=UTF-16")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      @Ignore("arq exception not translated")
      public void syntax() throws Exception {
        given()
            .queryParam("query", "ASK {")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }
    }
  }

  @RunWith(Enclosed.class)
  public static class Update {
    public static class Negative {
      @Test
      public void get() throws Exception {
        given()
            .queryParam("update", "CLEAR ALL")
        .when()
            .get()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void bad_multiple_updates() throws Exception {
        given()
            .formParam("update", "CLEAR NAMED", "CLEAR DEFAULT")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void wrong_media_type() throws Exception {
        given()
            .body(bytes("CLEAR NAMED"))
            .contentType("text/plain")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void missing_form_type() throws Exception {
        given() // rest assured cannot serialize without content type
            .body(bytes("update=CLEAR%20NAMED"))
            .contentType("")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void missing_direct_type() throws Exception {
        given()
            .body(bytes("CLEAR NAMED"))
            .contentType("")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void non_utf8() throws Exception {
        given()
            .body("CLEAR NAMED".getBytes(Charsets.UTF_16))
            .contentType("application/sparql-update; charset=UTF-16")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void syntax() throws Exception {
        given()
            .formParam("update", "CLEAR%20XYZ")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }

      @Test
      public void dataset_conflict() throws Exception {
        given()
            .formParam("using-named-graph-uri", "http://example/people")
            .formParam("update",
                "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>\n"
                    + "WITH <http://example/addresses>\n"
                    + "DELETE { ?person foaf:givenName 'Bill' }\n"
                    + "INSERT { ?person foaf:givenName 'William' }\n"
                    + "WHERE {\n"
                    + "?person foaf:givenName 'Bill'\n"
                    + "}")
        .when()
            .post()
        .then()
            .statusCode(indicates(HttpCode.CLIENT_ERROR));
      }
    }
  }

  // @formatter:on
}
