package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.io.TeeOutputStream;
import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.web.HttpExchangeReport;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.junit.rules.ExternalResource;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static java.util.Objects.requireNonNull;

/**
 * Manage a WebTarget instance for a set service endpoint.
 */
@NotThreadSafe
public final class ManagedClient extends ExternalResource implements Interactions.Report {
  private static final HostnameVerifier ALLOW_ALL = new HostnameVerifier() {
    @Override
    public boolean verify(final String host, final SSLSession ssl) {
      return true;
    }
  };

  public static ManagedClientBuilder create() {
    return new ManagedClientBuilder();
  }

  private final ClientBuilder builder;
  private final URI serviceAddress;
  private final HttpExchangeReport capturedExchanges;
  private Client client;

  private ManagedClient(final ClientBuilder builder, final URI serviceAddress) {
    this.builder = requireNonNull(builder);
    this.serviceAddress = requireNonNull(serviceAddress);
    this.capturedExchanges = HttpExchangeReport.create();
    this.builder.register(new MonitoringFilter(capturedExchanges));
  }

  public WebTarget endpoint() {
    return target(serviceAddress);
  }

  public WebTarget target(final URI address) {
    assert client != null : "JAX-RS client not initialized";
    return client.target(address);
  }

  @Override
  protected void before() {
    client = builder.build();
  }

  @Override
  protected void after() {
    client.close();
  }

  @Override
  public Appendable appendTo(final Appendable sink) throws IOException {
    capturedExchanges.appendTo(sink);
    return sink;
  }

  @Override
  public String toString() {
    return "ManagedClient{" + serviceAddress + '}';
  }

  @Provider
  @NotThreadSafe
  private static final class MonitoringFilter implements ClientRequestFilter, ClientResponseFilter, WriterInterceptor {

    private final HttpExchangeReport reporter;

    private MonitoringFilter(final HttpExchangeReport reporter) {
      this.reporter = reporter;
    }

    @Override
    public void filter(final ClientRequestContext request) throws IOException {
      reporter.captureRequest(request.getMethod(), request.getUri(), request.getStringHeaders());
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) throws IOException {
      final InputStream entityStream = response.getEntityStream();
      final byte[] data;
      if (entityStream != null) {
        data = ByteStreams.toByteArray(entityStream);
        response.setEntityStream(new ByteArrayInputStream(data));
      } else {
        data = "NONE".getBytes(Charsets.UTF_8);
      }
      reporter.captureResponse(response.getStatus(), response.getHeaders()).withResponseBody(data);
    }

    @Override
    public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
      final OutputStream out = context.getOutputStream();
      final TeeOutputStream interceptor = TeeOutputStream.wrap(out);
      context.setOutputStream(interceptor);
      context.proceed();
      reporter.withRequestBody(interceptor.captured());
    }
  }

  // FIXME : externalize client building
  public final static class ManagedClientBuilder {
    private ClientBuilder builder;

    public ManagedClientBuilder() {
      builder = ClientBuilder.newBuilder();
    }

    public ManagedClientBuilder use(final Object provider) {
      builder.register(provider);
      return this;
    }

    public ManagedClientBuilder secured(final KeyStore keyStore) {
      try {
        final TrustManagerFactory trustFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(keyStore);
        final TrustManager[] trustManagers = trustFactory.getTrustManagers();

        final SSLContext context = SSLContext.getInstance("TLS");
        // null KeyManager[] : not required for clients
        // null SecureRandom : default is used
        context.init(null, trustManagers, null);

        builder
            .hostnameVerifier(ALLOW_ALL)
            .sslContext(context);
      } catch (GeneralSecurityException e) {
        Throwables.propagate(e);
      }
      return this;
    }

    public ManagedClient build(final URI serviceAddress) {
      return new ManagedClient(builder, serviceAddress);
    }
  }
}
