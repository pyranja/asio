package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.web.EventSource;
import com.google.common.io.BaseEncoding;
import com.jayway.restassured.authentication.PreemptiveBasicAuthScheme;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Deployment specific configuration for rest-assured integration tests.
 */
public abstract class AsioSpec {

  public static final String EVENTS_ENDPOINT = "events";
  public static final String DEFAULT_SCHEMA = "schema";

  /**
   * Authorization is applied by sending the permission in the request headers.
   *
   * @param address base service address
   * @param header name of the auth bearing header
   * @return header based asio spec
   */
  public static AsioSpec withHeaderAuthorization(final URI address, final String header) {
    return new HeaderBased(address, header);
  }

  /**
   * Authorization is applied by including the permission in the request URI.
   *
   * @param address base service address
   * @return uri based asio spec
   */
  public static AsioSpec withUriAuthorization(final URI address) {
    return new UriBased(address);
  }

  /**
   * Authorization is skipped.
   *
   * @param address base service address
   * @return asio spec without authorization
   */
  public static AsioSpec withoutAuthorization(final URI address) {
    return new NoAuth(address);
  }

  private final URI root;
  private String schema = "";

  private AsioSpec(final URI root) {
    this.root = requireNonNull(root);
  }

  public AsioSpec useSchema(final String schema) {
    this.schema = schema;
    return this;
  }

  protected URI getRoot() {
    return root.resolve(schema);
  }

  /**
   * Create rest-assured request to the service root with given permission.
   *
   * @param permission required for the test request
   * @return initialized request specification
   */
  public RequestSpecification requestWith(final String permission) {
    requireNonNull(permission);
    final RequestSpecBuilder request = new RequestSpecBuilder().setBaseUri(getRoot());
    final RequestSpecBuilder authorized = authorize(request, permission);
    return authorized.build();
  }

  protected abstract RequestSpecBuilder authorize(final RequestSpecBuilder spec, final String permission);

  public abstract URI authorizedAddress(final String permission);

  public abstract EventSource eventSource();

  public abstract boolean isAuthorizing();

  /**
   * Send permission as http header and force basic auth.
   */
  private static class HeaderBased extends AsioSpec {
    private final String headerName;
    private final PreemptiveBasicAuthScheme defaultAuth;

    public static final String CREDENTIALS =
        BaseEncoding.base64().encode(Payload.encodeUtf8("anonymous:none"));

    private HeaderBased(final URI root, final String headerName) {
      super(root);
      this.headerName = requireNonNull(headerName, "header name");
      defaultAuth = new PreemptiveBasicAuthScheme();
      defaultAuth.setUserName("anonymous");
      defaultAuth.setPassword("none");
    }

    @Override
    protected RequestSpecBuilder authorize(final RequestSpecBuilder spec, final String permission) {
      return spec.setAuth(defaultAuth).addHeader(headerName, permission);
    }

    @Override
    public URI authorizedAddress(final String permission) {
      return this.getRoot();
    }

    @Override
    public EventSource eventSource() {
      final DefaultHttpClient httpClient = EventSource.defaultClient();
      httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
          request.addHeader(headerName, "admin");
          request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + CREDENTIALS);
        }
      });
      return EventSource.listenTo(this.getRoot().resolve(EVENTS_ENDPOINT), httpClient);
    }

    @Override
    public boolean isAuthorizing() {
      return true;
    }
  }

  /**
   * Use the URI based authorization mechanism
   */
  private static class UriBased extends AsioSpec {
    private UriBased(final URI root) {
      super(root);
    }

    @Override
    protected RequestSpecBuilder authorize(final RequestSpecBuilder spec, final String permission) {
      return spec.setBaseUri(authorizedAddress(permission));
    }

    @Override
    public URI authorizedAddress(final String permission) {
      return this.getRoot().resolve(permission + "/");
    }

    @Override
    public EventSource eventSource() {
      return EventSource.listenTo(authorizedAddress("admin").resolve(EVENTS_ENDPOINT), EventSource.defaultClient());
    }

    @Override
    public boolean isAuthorizing() {
      return true;
    }
  }


  /**
   * Do not modify requests, no authorization is applied
   */
  private static class NoAuth extends AsioSpec {
    private NoAuth(final URI root) {
      super(root);
    }

    @Override
    protected RequestSpecBuilder authorize(final RequestSpecBuilder spec, final String ignored) {
      return spec;
    }

    @Override
    public URI authorizedAddress(final String permission) {
      return this.getRoot();
    }

    @Override
    public EventSource eventSource() {
      return EventSource.listenTo(this.getRoot().resolve(EVENTS_ENDPOINT), EventSource.defaultClient());
    }

    @Override
    public boolean isAuthorizing() {
      return false;
    }
  }
}
