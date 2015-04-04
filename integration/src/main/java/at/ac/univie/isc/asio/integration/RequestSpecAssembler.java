package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.restassured.ReportingFilter;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.authentication.PreemptiveBasicAuthScheme;
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
    RequestSpecBuilder request;
    if (dsl.isManage()) {
      request = managementRequest(config.serviceBase.resolve(config.managementService));
    } else if (dsl.hasSchema()) {
      request = datasetRequest(dsl, config.serviceBase.resolve(dsl.getSchemaPath()));
    } else {
      request = datasetRequest(dsl, config.serviceBase);
    }
    final ReportingFilter reportingInterceptor = interactions.attached(ReportingFilter.create());
    return RestAssured.given().spec(request.build()).filter(reportingInterceptor);
  }

  private RequestSpecBuilder datasetRequest(final IntegrationDsl dsl, final URI baseUri) {
    final URI authedBaseUri = config.auth.configureUri(baseUri, dsl.getRole());
    RequestSpecBuilder request = new RequestSpecBuilder().setBaseUri(authedBaseUri.toString());
    request = config.auth.configureRequestSpec(request, dsl.getRole());

    final UsernamePasswordCredentials delegated = dsl.getDelegated();
    if (delegated != null) {
      request = config.auth.attachCredentials(delegated.getUserName(), delegated.getPassword(), request);
    }

    return request;
  }

  private RequestSpecBuilder managementRequest(final URI baseUri) {
    final PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
    scheme.setUserName(config.rootCredentials.getUserName());
    scheme.setPassword(config.rootCredentials.getPassword());
    return new RequestSpecBuilder()
        .setBaseUri(baseUri.toString())
        .setAuth(scheme);
  }
}
