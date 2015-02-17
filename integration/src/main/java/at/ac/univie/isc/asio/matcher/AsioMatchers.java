package at.ac.univie.isc.asio.matcher;

import com.google.common.collect.Table;
import com.google.common.net.MediaType;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.sun.net.httpserver.HttpExchange;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openjena.riot.Lang;

import java.util.Locale;

import static org.hamcrest.Matchers.*;

public final class AsioMatchers {

  // sql

  public static Matcher<String> sqlCsvEqualTo(final Table<Integer, String, String> expected) {
    return new SqlResultMatcher.Csv(expected);
  }

  public static Matcher<String> sqlWebrowsetEqualTo(final Table<Integer, String, String> expected) {
    return new SqlResultMatcher.Webrowset(expected);
  }

  // jena

  public static Matcher<String> rdfXmlEqualTo(final Model expected) {
    return new RdfModelMatcher(expected, Lang.RDFXML);
  }

  public static Matcher<String> rdfJsonEqualTo(final Model expected) {
    return new RdfModelMatcher(expected, Lang.RDFJSON);
  }

  public static Matcher<String> sparqlXmlEqualTo(final ResultSet expected) {
    return SparqlResultMatcher.create(expected, ResultsFormat.FMT_RS_XML);
  }

  public static Matcher<String> sparqlJsonEqualTo(final ResultSet expected) {
    return SparqlResultMatcher.create(expected, ResultsFormat.FMT_RS_JSON);
  }

  public static Matcher<String> sparqlCsvEqualTo(final ResultSet expected) {
    return SparqlResultMatcher.create(expected, ResultsFormat.FMT_RS_CSV);
  }

  // web

  /**
   * handles 'type/subtype+extension' format
   */
  public static Matcher<String> compatibleTo(final String rawExpectedType) {
    return new MimeMatcher(MediaType.parse(rawExpectedType.toLowerCase(Locale.ENGLISH)));
  }

  // mock http server

  public static Matcher<HttpExchange> basicAuthUsername(final String username) {
    return new ExpectCredentials(equalTo(username), Matchers.any(String.class));
  }

  public static Matcher<HttpExchange> basicAuthPassword(final String password) {
    return new ExpectCredentials(Matchers.any(String.class), equalTo(password));
  }

  private AsioMatchers() { /* factory class */ }
}
