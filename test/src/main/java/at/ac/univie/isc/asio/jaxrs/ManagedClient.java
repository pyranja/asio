package at.ac.univie.isc.asio.jaxrs;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Manage a WebTarget instance for a set service endpoint.
 */
@NotThreadSafe
public final class ManagedClient implements TestRule {
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
  private Client client;

  private ManagedClient(final ClientBuilder builder, final URI serviceAddress) {
    this.builder = requireNonNull(builder);
    this.serviceAddress = requireNonNull(serviceAddress);
    this.builder.register(new MonitoringFilter());
  }

  public WebTarget endpoint() {
    return target(serviceAddress);
  }

  public WebTarget target(final URI address) {
    assert client != null : "JAX-RS client not initialized";
    return client.target(address);
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        before();
        try {
          base.evaluate();
        } catch (AssumptionViolatedException skipMe) {
          throw skipMe;
        } catch (AssertionError failure) {
          throw new TestFailedReport(messageWithExchangeReport(failure), failure);
        } catch (Throwable error) {
          throw new TestInErrorReport(messageWithExchangeReport(error), error);
        } finally {
          after();
        }
      }
    };
  }

  private String messageWithExchangeReport(final Throwable error) {
    final String report = MonitoringFilter.findReport(client);
    return String.format(Locale.ENGLISH, "%s%n%s", error.getMessage(), report);
  }

  private static class TestInErrorReport extends RuntimeException {
    TestInErrorReport(final String message, final Throwable error) {
      super(error.getClass().getName() +": " + message);
      this.setStackTrace(error.getStackTrace());
      this.addSuppressed(error);
    }
  }

  private static class TestFailedReport extends AssertionError {
    TestFailedReport(final String message, final Throwable failure) {
      super(message);
      this.setStackTrace(failure.getStackTrace());
      this.addSuppressed(failure);
    }
  }

  private void before() {
    client = builder.build();
  }

  private void after() {
    client.close();
  }

  @Provider
  @NotThreadSafe
  private static final class MonitoringFilter implements ClientRequestFilter, ClientResponseFilter, WriterInterceptor {
    public static final String REPORT_KEY = "at.ac.univie.isc.asio.exchange.report";

    private final ExchangeReporter reporter = ExchangeReporter.create();

    public static String findReport(Client client) {
      final Object reporter = client.getConfiguration().getProperty(REPORT_KEY);
      if (reporter != null && reporter instanceof ExchangeReporter) {
        return ((ExchangeReporter) reporter).format();
      } else {
        return "NO EXCHANGE CAPTURED";
      }
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
      requestContext.getClient().property(REPORT_KEY, reporter);
      reporter.update(requestContext);
    }

    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) throws IOException {
      final InputStream entityStream = responseContext.getEntityStream();
      final byte[] body;
      if (entityStream != null) {
        body = ByteStreams.toByteArray(entityStream);
        responseContext.setEntityStream(new ByteArrayInputStream(body));
      } else {
        body = "NONE".getBytes(Charsets.UTF_8);
      }
      reporter.update(responseContext).responseBody(body);
    }

    @Override
    public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
      final OutputStream out = context.getOutputStream();
      final TeeOutputStream interceptor = TeeOutputStream.wrap(out);
      context.setOutputStream(interceptor);
      context.proceed();
      reporter.requestBody(interceptor.captured());
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
