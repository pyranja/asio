package at.ac.univie.isc.asio.web;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

/**
 * Helpers for working with URIs.
 */
public final class WebTools {

  /**
   * Ensure the given URI ends with a {@code '/'}, i.e. it points to the root of a directory,
   * not to a document. If no path is defined, it is set to the root path ({@code '/'}).
   *
   * @return URI where the path ends with '/'
   */
  public static URI ensureDirectoryPath(final URI it) {
    final String path = it.getPath();
    if (isDocumentPath(path)) {
      return cloneWithPath(it, path + "/");
    }
    return it;
  }

  private static boolean isDocumentPath(final String path) {
    return path != null && !path.endsWith("/");
  }

  private static URI cloneWithPath(final URI it, final String path) {
    try {
      return new URI(it.getScheme(), it.getUserInfo(), it.getHost(), it.getPort(), path, it.getQuery(), it.getFragment());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /** a ssl hostname verifier, that will verify any hostname */
  public static HostnameVerifier allowAllVerifier() {
    return new AllowAllVerifier();
  }

  /** array containing a ssl trust manager, that will trust any server certificate */
  public static TrustManager[] trustAnyManager() {
    return new TrustManager[] { new NoopTrustManager() };
  }

  private static final class AllowAllVerifier implements HostnameVerifier {
    @Override
    public boolean verify(final String s, final SSLSession sslSession) {
      return true;
    }

  }
  private static final class NoopTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) {}
    @Override
    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) {}
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}
