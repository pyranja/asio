package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalToIgnoringCase;

@RunWith(Parameterized.class)
@Category(Integration.class)
public class FeatureRoles extends IntegrationTest {

  @Parameterized.Parameters(name = "{index}: using role {0} for login")
  public static Iterable<Object[]> roles() {
    return Arrays.asList(new Object[][] {
        { "read" }
        , { "user" }
        , { "full" }
        , { "owner" }
        , { "admin" }
    });
  }

  @Parameterized.Parameter(0)
  public String role;

  @Before
  public void skipIfNotSecured() {
    ensureSecured();
  }

  // @formatter:off

  @Test
  public void login_is_role_name() {
    given().role(role)
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("login", equalToIgnoringCase(role))
    ;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void authorities_contain_role() {
    given().role(role)
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("authorities", Matchers.hasItem(equalToIgnoringCase("ROLE_" + role)))
    ;
  }
}
