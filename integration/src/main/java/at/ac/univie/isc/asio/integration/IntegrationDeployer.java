package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.ByteSource;
import com.google.common.net.HttpHeaders;
import org.apache.http.HttpStatus;

/**
 * Deploy asio container for tests.
 */
public final class IntegrationDeployer {
  private final IntegrationDsl dsl;

  IntegrationDeployer(final IntegrationDsl dsl) {
    this.dsl = dsl;
  }

  public void fromD2r(final String name, final ByteSource mapping) {
    dsl.manage().role("admin").and()
      .header(HttpHeaders.ACCEPT, "application/json")
      .contentType("text/turtle")
      .content(Payload.asArray(mapping))
    .when()
      .put("catalog/{schema}", name)
    .then()
      .statusCode(HttpStatus.SC_CREATED);
  }

  public void fromJson(final String name, final String json) {
    dsl.manage().role("admin").and()
      .header(HttpHeaders.ACCEPT, "application/json")
      .contentType("application/json")
      .content(json)
    .when()
      .put("catalog/{schema}", name)
    .then()
      .statusCode(HttpStatus.SC_CREATED);
  }
}
