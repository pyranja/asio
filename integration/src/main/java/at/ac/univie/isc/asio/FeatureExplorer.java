package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.Matchers.is;

@Category(Integration.class)
public class FeatureExplorer extends IntegrationTest {

  // @formatter:off

  @Test
  public void delivers_sql_browser_page() throws Exception {
    ensureLanguageSupported("sql");
    given().role("read").spec()
    .when()
      .get("explore/sql.html")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/html"));
  }

  @Test
  public void delivers_sparql_browser_page() throws Exception {
    ensureLanguageSupported("sparql");
    given().role("read").spec()
    .when()
      .get("explore/sparql.html")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/html"));
  }

  @Test
  public void delivers_events_page() throws Exception {
    given().role("read").spec()
    .when()
      .get("explore/events.html")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/html"));
  }
}
