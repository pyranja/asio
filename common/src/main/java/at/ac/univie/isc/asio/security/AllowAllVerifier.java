package at.ac.univie.isc.asio.security;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Perform no checks on the authenticity of the hostname.
 * <strong>Attention:</strong> This effectively nullifies any security offered by TLS.
 */
public final class AllowAllVerifier implements HostnameVerifier {
  public static AllowAllVerifier instance() {
    return new AllowAllVerifier();
  }

  private AllowAllVerifier() {}

  @Override
  public boolean verify(final String s, final SSLSession sslSession) {
    return true;
  }
}
