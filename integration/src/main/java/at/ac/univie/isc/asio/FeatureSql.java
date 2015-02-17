package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.web.HttpCode;
import com.google.common.collect.ImmutableTable;
import com.google.common.net.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.AsioMatchers.sqlCsvEqualTo;
import static at.ac.univie.isc.asio.web.HttpMatchers.indicates;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringCase;

/**
 * Extended functionality of SQL protocol endpoints.
 */
@Category(Integration.class)
public class FeatureSql extends IntegrationTest {
  
  private static final String NOOP_UPDATE = "DROP TABLE IF EXISTS test_table_gaga";
  private static final String NOOP_SELECT = "SELECT 1 AS RESULT";

  // @formatter:off

  @Test
  public void deny_update_with_read_permission() throws Exception {
    givenPermission("read")
      .formParam("update", NOOP_UPDATE)
    .when()
      .post("/sql")
    .then()
      .statusCode(is(HttpStatus.SC_FORBIDDEN));
  }

  public class Execute {
    @Test
    public void select() throws Exception {
      givenPermission("read")
        .header(HttpHeaders.ACCEPT, "text/csv")
        .param("query", "SELECT 1 AS RESULT")
      .when()
        .post("/sql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .content(is(sqlCsvEqualTo(ImmutableTable.of(0, "RESULT", "1"))));
    }

    @Test
    public void update() throws Exception {
      givenPermission("full")
        .header(HttpHeaders.ACCEPT, "application/xml")
        .param("update", "DROP TABLE IF EXISTS test_table_gaga")
      .when()
        .post("/sql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .content("sql.head.@statement", is("DROP TABLE IF EXISTS test_table_gaga"))
        .content("sql.update.@affected", is("0"));
    }
  }

  public class Schema {
    @Test
    public void redirect_to_metadata_resource_for_schema() throws Exception {
      final String expectedRedirection = serviceAddress("read").resolve("meta/schema").toString();
      givenPermission("read")
        .redirects().follow(false)
      .when()
        .get("/sql/schema")
      .then()
        .statusCode(is(HttpStatus.SC_MOVED_PERMANENTLY))
        .header(HttpHeaders.LOCATION, equalToIgnoringCase(expectedRedirection));
    }

    @Test
    public void deliver_xml() throws Exception {
      givenPermission("read")
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .get("/meta/schema")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/xml"));
    }

    @Test
    public void deliver_json() throws Exception {
      givenPermission("read")
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .get("/meta/schema")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/json"));
    }

    @Test
    public void reject_unauthorized_access() throws Exception {
      ensureSecured();
      givenPermission("none")
      .when()
        .get("/meta/schema")
      .then()
        .statusCode(is(HttpStatus.SC_FORBIDDEN));
    }
  }
  
  public class MediaType {
    @Test
    public void select_for_webrowset() throws Exception {
      givenPermission("read")
        .formParam("query", NOOP_SELECT)
        .header(HttpHeaders.ACCEPT, "application/webrowset+xml")
      .when()
        .post("/sql")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/webrowset+xml"));
    }
  
    @Ignore("sql-results+xml not implemented")
    @Test
    public void select_for_xml() throws Exception {
      givenPermission("read")
        .formParam("query", NOOP_SELECT)
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .post("/sql")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/sql-results+xml"));
    }
  
    @Ignore("sql-results+json not implemented")
    @Test
    public void select_for_json() throws Exception {
      givenPermission("read")
        .formParam("query", NOOP_SELECT)
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .post("/sql")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/sql-results+json"));
    }

    @Test
    public void update_for_xml() throws Exception {
      givenPermission("full")
        .formParam("update", NOOP_UPDATE)
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .post("/sql")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/sql-results+xml"));
    }
  
    @Ignore("sql-results+json not implemented")
    @Test
    public void update_for_json() throws Exception {
      givenPermission("full")
        .formParam("update", NOOP_UPDATE)
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .post("/sql")
      .then()
        .statusCode(indicates(HttpCode.SUCCESSFUL))
        .contentType(is("application/sql-results+json"));
    }
  }
}
