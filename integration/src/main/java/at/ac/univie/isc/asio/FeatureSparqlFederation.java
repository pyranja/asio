package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.web.HttpServer;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;
import com.hp.hpl.jena.xmloutput.impl.Basic;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.*;
import static org.hamcrest.CoreMatchers.is;

/**
 * Verify functionality of a SPARQL basic federation processor.
 * <p>
 * Test cases use federated queries against mock servers on {@code localhost}.
 * </p>
 */
@Category(Integration.class)
@RunWith(Parameterized.class)
public class FeatureSparqlFederation extends IntegrationTest {

  @Parameterized.Parameters(name = "case {index} : {0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"no_remote_trivial"}
        , {"single_remote_simple"}
        , {"both_remotes_join"}
        , {"both_remotes_optional_join"}
    });
  }

  @Parameterized.Parameter(0)
  public String testCase;

  @Rule
  public final HttpServer http = interactions.attached(Rules.httpServer("sparql-endpoints"));
  private Map<String, URI> endpoints = new HashMap<>();

  @Before
  public void prepare_endpoints() throws IOException {
    try (final InputStream namesSource = Classpath.fetch("sparql/endpoint-names.ttl")) {
      final Model remoteNamesModel =
          ModelFactory.createDefaultModel().read(namesSource, null, "TURTLE");
      http.with("/names", SparqlProcessor.using(remoteNamesModel));
      endpoints.put("endpoint.names", http.address().resolve("names"));
    }
    try (final InputStream emailsSource = Classpath.fetch("sparql/endpoint-emails.ttl")) {
      final Model remoteEmailsModel =
          ModelFactory.createDefaultModel().read(emailsSource, null, "TURTLE");
      http.with("/emails", SparqlProcessor.using(remoteEmailsModel));
      endpoints.put("endpoint.emails", http.address().resolve("emails"));
    }
  }

  /** read the query template for current test case */
  private String query() throws IOException {
    final String query = Classpath.read(Pretty.format("sparql/federation/%s.rq", testCase));
    return Pretty.substitute(query, endpoints);
  }

  /** read the expected results for current test case */
  private ResultSet expected() throws IOException {
    return ResultSetFactory.fromXML(Classpath.fetch(Pretty.format("sparql/federation/%s.srx", testCase)));
  }

  // @formatter:off

  @Test
  public void query_yields_expected_xml_result() throws Exception {
    givenPermission("read")
      .header(HttpHeaders.ACCEPT, "application/xml")
      .formParam("query", query())
    .when()
      .post("/sparql")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/sparql-results+xml"))
      .body(is(sparqlXmlEqualTo(expected())));
  }

  @Test
  public void query_yields_expected_json_result() throws Exception {
    givenPermission("read")
      .header(HttpHeaders.ACCEPT, "application/json")
      .formParam("query", query())
    .when()
      .post("/sparql")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/sparql-results+json"))
      .body(is(sparqlJsonEqualTo(expected())));
  }

  @Test
  public void query_yields_expected_csv_result() throws Exception {
    givenPermission("read")
      .header(HttpHeaders.ACCEPT, "text/csv")
      .formParam("query", query())
    .when()
      .post("/sparql")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/csv"))
      .body(is(sparqlCsvEqualTo(expected())));
  }

  // @formatter:on

  /** The fake sparql handler simulating remote sparql endpoints */
  private static class SparqlProcessor implements HttpHandler {
    public static SparqlProcessor using(final Model model) {
      return new SparqlProcessor(model);
    }

    private static final XMLOutput XML_RESULTS = new XMLOutput();
    private static final Basic XML_MODEL = new Basic();

    private final Model model;

    private SparqlProcessor(final Model model) {
      this.model = model;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException {
      final String query = HttpServer.parseParameters(exchange).get("query");
      final QueryExecution execution = QueryExecutionFactory.create(query, model);
      switch (execution.getQuery().getQueryType()) {
        case Query.QueryTypeAsk:
          final Boolean result = execution.execAsk();
          sendSuccess(exchange, "application/sparql-results+xml");
          XML_RESULTS.format(exchange.getResponseBody(), result);
          break;
        case Query.QueryTypeSelect:
          final ResultSet resultSet = execution.execSelect();
          sendSuccess(exchange, "application/sparql-results+xml");
          XML_RESULTS.format(exchange.getResponseBody(), resultSet);
          break;
        case Query.QueryTypeConstruct:
          final Model construct = execution.execConstruct();
          sendSuccess(exchange, "application/rdf+xml");
          XML_MODEL.write(construct, exchange.getResponseBody(), null);
          break;
        case Query.QueryTypeDescribe:
          final Model description = execution.execDescribe();
          sendSuccess(exchange, "application/rdf+xml");
          XML_MODEL.write(description, exchange.getResponseBody(), null);
          break;
        default:
          throw new IllegalArgumentException("unknown query type");
      }
      exchange.close();
    }

    private void sendSuccess(final HttpExchange exchange, final String contentType) throws IOException {
      exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
      exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);
    }
  }
}
