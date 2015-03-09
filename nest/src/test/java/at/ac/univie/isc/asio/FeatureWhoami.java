package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.security.Role;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.experimental.categories.Category;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Set;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.Matchers.*;

@RunWith(Theories.class)
@Category(Integration.class)
public class FeatureWhoami extends IntegrationTest {

  @DataPoints
  public static Role[] roles() {
    return Role.values();
  }

  @DataPoint
  public static Identity credentials = Identity.from("user", "password");

  // @formatter:off

  @Theory
  public void delivers_json(final Role role) {
    given().manage().role(role.name())
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/json"))
    ;
  }

  @Theory
  public void login_is_role_name(final Role role) {
    given().manage().role(role.name())
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("login", equalToIgnoringCase(role.name()))
    ;
  }

  @Theory
  @SuppressWarnings("unchecked")
  public void permissions_contains_all_granted_to_role(final Role role) {
    final Set<String> expected = AuthorityUtils.authorityListToSet(role.getGrantedAuthorities());
    given().manage().role(role.name())
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("permissions", containsInAnyOrder(expected.toArray()))
    ;
  }

  @Theory
  public void has_no_identity_if_no_delegated_credentials_present(final Role role) {
    given().manage().role(role.name())
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("name", is(nullValue()))
      .body("secret", is(nullValue()))
    ;
  }

  @Theory
  public void contains_delegated_credentials(final Role role, final Identity credentials) {
    given().manage().role(role.name()).delegate(credentials.getName(), credentials.getSecret())
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("name", is(credentials.getName()))
      .body("secret", is(credentials.getSecret()))
    ;
  }
}
