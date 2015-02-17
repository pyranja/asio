package at.ac.univie.isc.asio;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@Category(Integration.class)
public class FeatureMetadata extends IntegrationTest {

  // @formatter:off

  public class DatasetMetadata {

    @Test
    public void deliver_xml() throws Exception {
      givenPermission("read")
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .get("/meta")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/xml"))
        .root("dataset")
        .body("globalID", not(isEmptyOrNullString()))
        .body("localID", not(isEmptyOrNullString()))
        .body("sparqlEndPoint", not(isEmptyOrNullString()))
        .body("name", not(isEmptyOrNullString()))
        .body("type", is("Dataset"))
        .body("status", not(isEmptyOrNullString()));
    }

    @Test
    public void deliver_json() throws Exception {
      givenPermission("read")
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .get("/meta")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/json"))
        .root("dataset")
        .body("globalID", not(isEmptyOrNullString()))
        .body("localID", not(isEmptyOrNullString()))
        .body("sparqlEndPoint", not(isEmptyOrNullString()))
        .body("name", not(isEmptyOrNullString()))
        .body("type", is("Dataset"))
        .body("status", not(isEmptyOrNullString()));
    }

    @Test
    public void reject_unauthorized_access() throws Exception {
      ensureSecured();
      givenPermission("none")
      .when()
        .get("/meta")
      .then()
        .statusCode(is(HttpStatus.SC_FORBIDDEN));
    }
  }
}
