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
package at.ac.univie.isc.asio.security;

import com.jayway.restassured.authentication.PreemptiveBasicAuthScheme;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.apache.http.auth.Credentials;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Authenticate via basic authentication scheme, with fixed credentials.
 */
final class BasicAuthMechanism extends AuthMechanism {
  private final Credentials credentials;

  public BasicAuthMechanism(final Credentials secret) {
    this.credentials = requireNonNull(secret, "credentials");
  }

  @Override
  public boolean isAuthorizing() {
    // used credentials always grant full access rights
    return false;
  }

  @Override
  public URI configureUri(final URI uri, final String ignored) {
    return uri;
  }

  @Override
  public RequestSpecBuilder configureRequestSpec(final RequestSpecBuilder spec, final String ignored) {
    final PreemptiveBasicAuthScheme scheme = new PreemptiveBasicAuthScheme();
    scheme.setUserName(credentials.getUserPrincipal().getName());
    scheme.setPassword(credentials.getPassword());
    return spec.setAuth(scheme);
  }
}
