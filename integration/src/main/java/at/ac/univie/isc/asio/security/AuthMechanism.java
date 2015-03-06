package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.BaseEncoding;
import com.jayway.restassured.builder.RequestSpecBuilder;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.client.AbstractHttpClient;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Define extension methods to adapt integration tests to different authorization mechanisms in
 * asio. When performing requests, the base uri {@code MUST} always be processed by
 * {@link #configureUri(java.net.URI, String)} before resolving request specific path components.
 * Depending on the utilized client either
 * {@link #configureRequestSpec(com.jayway.restassured.builder.RequestSpecBuilder, String)} or
 * {@link #configureClient(org.apache.http.impl.client.AbstractHttpClient, String)}} {@code MUST} be
 * used to apply the mechanism to the client before invoking the request.
 */
public abstract class AuthMechanism {
  /**
   * Authenticate via basic authentication scheme, where the username is equal to the required role
   * and the configured, fixed password.
   *
   * @param secret fixed authorization password
   */
  public static AuthMechanism basic(final String secret) {
    return new BasicAuthMechanism(secret);
  }

  /**
   * Skip authorization.
   */
  public static AuthMechanism none() {
    return new NullAuthMechanism();
  }

  /**
   * Authorize requests by injecting the role into the request uri.
   */
  public static AuthMechanism uri() {
    return new UriAuthMechanism();
  }

  protected String credentialDelegationHeader = HttpHeaders.AUTHORIZATION;

  public final AuthMechanism overrideCredentialDelegationHeader(final String headerName) {
    this.credentialDelegationHeader = requireNonNull(headerName);
    return this;
  }

  // === util ======================================================================================

  /**
   * @return true if this mechanism performs authorization.
   */
  public final boolean isAuthorizing() {
    return ! (this instanceof NullAuthMechanism);
  }

  // === contract ==================================================================================

  /**
   * Inject authentication information into the request URI. Must be applied to the base service URI,
   * before resolving sub-resources.
   *
   * @param uri  base uri of request
   * @param role required role
   * @return uri with injected auth information
   */
  public URI configureUri(URI uri, String role) {
    return uri;
  }

  /**
   * Apply authentication mechanism to rest-assured request spec.
   *
   * @param spec spec builder
   * @param role required role
   * @return configured request spec
   */
  public RequestSpecBuilder configureRequestSpec(RequestSpecBuilder spec, String role) {
    return spec;
  }

  /**
   * Attach delegated credentials to the given request.
   *
   * @param username client id
   * @param secret secret for authentication
   * @return configured request
   */
  public RequestSpecBuilder attachCredentials(final String username, final String secret, final RequestSpecBuilder request) {
    final String credentials = BaseEncoding.base64().encode(Payload.encodeUtf8(username + ":" + secret));
    return request.addHeader(credentialDelegationHeader, "Basic " + credentials);
  }

  /**
   * Apply auth mechanism to apache http client.
   *
   * @param client   apache client
   * @param role     required role
   * @param <CLIENT> concrete type of http client for type safe configuration
   * @return configured http client
   */
  public <CLIENT extends AbstractHttpClient> CLIENT configureClient(CLIENT client, String role) {
    return client;
  }
}
