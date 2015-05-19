/*
 * #%L
 * asio integration
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
