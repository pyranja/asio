package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.EnvironmentSpec;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.FunctionalTest;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.tool.Pretty;
import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.web.HttpServer;
import at.ac.univie.isc.asio.web.HttpCode;
import com.google.common.base.Charsets;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;
import com.hp.hpl.jena.xmloutput.impl.Basic;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
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

import static at.ac.univie.isc.asio.web.HttpMatchers.indicates;
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.LogConfig.logConfig;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Verify functionality of a SPARQL basic federation processor.
 * <p>
 * Test cases use federated queries against mock servers on {@code localhost}.
 * </p>
 */
@RunWith(Parameterized.class)
@Category(FunctionalTest.class)
public class SparqlFederationTest {
  @BeforeClass
  public static void configureRestAssured() {
    baseURI = EnvironmentSpec.current.sparqlEndpoint().toString();
    config = config()
        .encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8.name()))
        .logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails())
    ;
  }

  @Parameterized.Parameters(name = "{index} : {0}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"no_remote_trivial" }
        , {"single_remote_simple"}
        , {"both_remotes_join"}
        , {"both_remotes_optional_join"}
    });
  }

  @Rule
  public final HttpServer http = Rules.httpServer("sparql-endpoints");
  @Rule
  public final Interactions collector = Rules.addReport(http);
  private Map<String, URI> endpoints = new HashMap<>();

  private final String label;

  public SparqlFederationTest(final String label) {
    this.label = label;
  }

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

  @Test
  public void query_yields_expected_results() throws Exception {
    final InputStream raw =
        given().header(HttpHeaders.ACCEPT, "application/xml").formParam("query", query())
            .when().post()
            .then().statusCode(indicates(HttpCode.SUCCESSFUL)).contentType(is("application/sparql-results+xml"))
            .extract().asInputStream();
    final ResultSet actual = ResultSetFactory.fromXML(raw);
    // XXX : create matcher
    assertThat(ResultSetCompare.equalsByValue(actual, expected()), is(true));
  }

  private String query() throws IOException {
    final String query = Classpath.read(Pretty.format("sparql/case/%s.rq", label));
    return Pretty.substitute(query, endpoints);
  }

  private ResultSet expected() throws IOException {
    return ResultSetFactory.fromXML(Classpath.fetch(Pretty.format("sparql/case/%s.srx", label)));
  }

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
