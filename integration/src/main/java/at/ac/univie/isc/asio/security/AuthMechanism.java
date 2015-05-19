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

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.BaseEncoding;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.AbstractHttpClient;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Define extension methods to adapt integration tests to different authorization mechanisms in
 * asio. When performing requests, the base uri {@code MUST} always be processed by
 * {@link #configureUri(java.net.URI, String)} before resolving request specific path components.
 * Depending on the utilized client either
 * {@link #configureRequestSpec(com.jayway.restassured.builder.RequestSpecBuilder, String)} or
 * {@link #applyBasicAuth(AbstractHttpClient, Credentials)}} {@code MUST} be
 * used to apply the mechanism to the client before invoking the request.
 */
public abstract class AuthMechanism {
  /**
   * Authorize requests by injecting the role into the request uri.
   */
  public static AuthMechanism uri() {
    return new UriAuthMechanism();
  }

  /**
   * Authenticate via basic authentication scheme with fixed credentials.
   */
  public static AuthMechanism basic(final String username, final String password) {
    return new BasicAuthMechanism(new UsernamePasswordCredentials(username, password));
  }

  /**
   * Skip authorization.
   */
  public static AuthMechanism none() {
    return new NullAuthMechanism();
  }

  private String credentialDelegationHeader = HttpHeaders.AUTHORIZATION;

  public final AuthMechanism overrideCredentialDelegationHeader(final String headerName) {
    this.credentialDelegationHeader = requireNonNull(headerName);
    return this;
  }

  // === util ======================================================================================

  /**
   * Attach delegated credentials to the given request.
   *
   * @param username client id
   * @param secret secret for authentication
   * @return configured request
   */
  public final RequestSpecBuilder attachCredentials(final String username, final String secret, final RequestSpecBuilder request) {
    final String credentials = BaseEncoding.base64().encode(Payload.encodeUtf8(username + ":" + secret));
    return request.addHeader(credentialDelegationHeader, "Basic " + credentials);
  }

  /**
   * Apply auth mechanism to apache http client.
   *
   * @param <CLIENT> concrete type of http client for type safe configuration
   * @param client   apache client
   * @param credentials     required role
   * @return configured http client
   */
  public final <CLIENT extends AbstractHttpClient> CLIENT applyBasicAuth(final CLIENT client, final Credentials credentials) {
    client.addRequestInterceptor(new PreemptiveAuthInterceptor(credentials));
    return client;
  }

  // === contract ==================================================================================

  /**
   * @return true if this mechanism performs authorization.
   */
  public abstract boolean isAuthorizing();

  /**
   * Inject authentication information into the request URI. Must be applied to the base service URI,
   * before resolving sub-resources.
   *
   * @param uri  base uri of request
   * @param role required role
   * @return uri with injected auth information
   */
  public abstract URI configureUri(URI uri, String role);

  /**
   * Apply authentication mechanism to rest-assured request spec.
   *
   * @param spec spec builder
   * @param role required role
   * @return configured request spec
   */
  public abstract RequestSpecBuilder configureRequestSpec(RequestSpecBuilder spec, String role);
}
