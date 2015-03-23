package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.restassured.ReportingFilter;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.auth.UsernamePasswordCredentials;

import java.net.URI;

/**
 * Combine configuration and dsl settings to create a rest-assured request specification.
 */
class RequestSpecAssembler implements IntegrationDsl.SpecFactoryCallback {
  private final IntegrationSettings config;
  private final Interactions interactions;

  public RequestSpecAssembler(final IntegrationSettings config, final Interactions interactions) {
    this.config = config;
    this.interactions = interactions;
  }

  @Override
  public RequestSpecification requestFrom(final IntegrationDsl dsl) {
    final URI baseUri;
    if (dsl.isManage()) {
      baseUri = config.serviceBase.resolve(config.managementService);
    } else if (dsl.hasSchema()) {
      baseUri = config.serviceBase.resolve(dsl.getSchemaPath());
    } else {
      baseUri = config.serviceBase;
    }
    final URI authedBaseUri = config.auth.configureUri(baseUri, dsl.getRole());

    RequestSpecBuilder request = new RequestSpecBuilder().setBaseUri(authedBaseUri.toString());
    request = config.auth.configureRequestSpec(request, dsl.getRole());

    final UsernamePasswordCredentials delegated = dsl.getDelegated();
    request = delegated == null
        ? request
        : config.auth.attachCredentials(delegated.getUserName(), delegated.getPassword(), request);

    final ReportingFilter reportingInterceptor = interactions.attached(ReportingFilter.create());
    return RestAssured.given().spec(request.build()).filter(reportingInterceptor);
  }
}
