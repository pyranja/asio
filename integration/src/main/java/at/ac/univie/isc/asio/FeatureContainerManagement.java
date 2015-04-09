package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

public class FeatureContainerManagement extends IntegrationTest {

  @Before
  public void skipIfNotSupported() {
    ensureContainerSupported();
  }

  // @formatter:off

  @Test
  public void lists_deployed_containers() throws Exception {
    given().role("admin").manage().and()
      .when()
        .get("container")
      .then()
        .statusCode(HttpStatus.SC_OK)
        .body("", not(empty()));
  }
}
