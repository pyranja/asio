package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Category(Integration.class)
public class FeatureWhoami extends IntegrationTest {
  // @formatter:off

  @Test
  public void delivers_json() {
    given()
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/json"))
    ;
  }

  @Test
  public void contains_delegated_credentials() {
    given().delegate("user", "password")
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("name", Matchers.is("user"))
      .body("secret", Matchers.is("password"))
    ;
  }

  @Test
  public void has_no_identity_if_no_delegated_credentials_present() {
    given()
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("name", is(nullValue()))
      .body("secret", is(nullValue()))
    ;
  }
}
