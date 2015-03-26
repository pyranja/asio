package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.ByteSource;
import com.google.common.net.HttpHeaders;
import com.jayway.restassured.filter.log.LogDetail;
import org.apache.http.HttpStatus;

/**
 * Deploy asio container for tests.
 */
public final class IntegrationDeployer {
  private final IntegrationDsl dsl;

  IntegrationDeployer(final IntegrationDsl dsl) {
    this.dsl = dsl;
  }

  public void d2rq(final String name, final ByteSource mapping) {
    dsl.manage().role("admin").and()
      .header(HttpHeaders.ACCEPT, "application/json")
      .contentType("text/turtle")
      .content(Payload.asArray(mapping))
    .when()
      .put("container/{schema}", name)
    .then()
      .log().ifValidationFails(LogDetail.ALL)
      .statusCode(HttpStatus.SC_CREATED);
  }
}
