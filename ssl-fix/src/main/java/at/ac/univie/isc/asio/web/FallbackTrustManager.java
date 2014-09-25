package at.ac.univie.isc.asio.web;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

import static java.util.Objects.requireNonNull;

/**
 * Consult a fallback {@code TrustManager} if the primary one does not trust.
 */
public final class FallbackTrustManager implements X509TrustManager {
  public static X509TrustManager chain(final X509TrustManager primary,
                                       final X509TrustManager fallback) {
    return new FallbackTrustManager(primary, fallback);
  }

  private final X509TrustManager primary;
  private final X509TrustManager fallback;

  private FallbackTrustManager(final X509TrustManager primary, final X509TrustManager fallback) {
    this.primary = requireNonNull(primary);
    this.fallback = requireNonNull(fallback);
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType)
      throws CertificateException {
    try {
      primary.checkClientTrusted(x509Certificates, authType);
    } catch (final CertificateException first) {
      try {
        fallback.checkClientTrusted(x509Certificates, authType);
      } catch (final CertificateException second) {
        first.addSuppressed(second);
        throw first;
      }
    }
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType)
      throws CertificateException {
    try {
      primary.checkServerTrusted(x509Certificates, authType);
    } catch (final CertificateException first) {
      try {
        fallback.checkServerTrusted(x509Certificates, authType);
      } catch (final CertificateException second) {
        first.addSuppressed(second);
        throw first;
      }
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    final X509Certificate[] primaryIssuers = primary.getAcceptedIssuers();
    final X509Certificate[] fallbackIssuers = fallback.getAcceptedIssuers();
    final X509Certificate[]
        merged =
        Arrays.copyOf(primaryIssuers, primaryIssuers.length + fallbackIssuers.length);
    System.arraycopy(fallbackIssuers, 0, merged, primaryIssuers.length, fallbackIssuers.length);
    return merged;
  }
}
