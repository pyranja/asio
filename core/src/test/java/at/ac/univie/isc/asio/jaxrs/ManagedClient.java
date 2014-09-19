package at.ac.univie.isc.asio.jaxrs;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.apache.cxf.transport.https.CertificateHostnameVerifier;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.*;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static java.util.Objects.requireNonNull;

/**
 * Manage a WebTarget instance for a set service endpoint.
 */
@NotThreadSafe
public final class ManagedClient implements TestRule {
  private static final Logger log = LoggerFactory.getLogger(ManagedClient.class);
  private static final Marker MONITOR = MarkerFactory.getMarker("MONITOR");

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
        } catch (AssumptionViolatedException e) {
          /* ignore skipped tests */
          throw e;
        } catch (AssertionError e) {
          failed(e, description);
          throw e;
        } catch (Throwable t) {
          error(t, description);
          throw t;
        } finally {
          after();
        }
      }
    };
  }

  private void before() {
    client = builder.build();
  }

  private void after() {
    client.close();
  }

  private void error(final Throwable t, final Description description) {
    final String report = MonitoringFilter.findReport(client);
    log.error(MONITOR, "{} failed with internal error {}\n{}", description, t.getMessage(), report);
  }

  private void failed(final AssertionError e, final Description description) {
    final String report = MonitoringFilter.findReport(client);
    log.error(MONITOR, "{} failed expectation {}\n{}", description, e.getMessage(), report);
  }

  @Provider
  @NotThreadSafe
  private static final class MonitoringFilter implements ClientRequestFilter, ClientResponseFilter {
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
      reporter.update(responseContext).with(body);
    }
  }

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
        TrustManagerFactory trustFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(keyStore);
        TrustManager[] trustManagers = trustFactory.getTrustManagers();

        final SSLContext context = SSLContext.getInstance("TLS");
        // null KeyManager[] : not required for clients
        // null SecureRandom : default is used
        context.init(null, trustManagers, null);

        builder
            .hostnameVerifier(CertificateHostnameVerifier.DEFAULT_AND_LOCALHOST)
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
