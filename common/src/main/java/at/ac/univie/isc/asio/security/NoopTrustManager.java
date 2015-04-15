package at.ac.univie.isc.asio.security;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * A TrustManager, which accepts any certificate without checking.
 * <strong>Attention:</strong> This effectively nullifies any security offered by TLS.
 */
public final class NoopTrustManager implements X509TrustManager {
  public static TrustManager[] asArray() {
    return new TrustManager[] { instance() };
  }

  public static NoopTrustManager instance() {
    return new NoopTrustManager();
  }

  private NoopTrustManager() {}

  @Override
  public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) {}
  @Override
  public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) {}
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}
