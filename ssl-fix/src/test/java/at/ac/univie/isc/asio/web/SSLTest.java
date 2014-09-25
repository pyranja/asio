package at.ac.univie.isc.asio.web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Sample usage of vph-truststore and hostname verification disabler
 */
public class SSLTest {

  public static final URI SHEFFIELD =
      URI.create("https://vphsharedata2.sheffield.ac.uk/vph_share_industry/read/sparql");
  public static final URI GOOGLE =
      URI.create("https://www.google.com");

  public static void main(final String... ignored) throws Exception {
    final KeyStore vphStore = SslUtils.loadKeystore(
        SslFixListener.VPH_STORE_PATH, SslFixListener.VPH_STORE_PASSWORD.toCharArray());
    final X509TrustManager vphTrust = SslUtils.createTrustManagerFrom(vphStore);
    final TrustManager chained = FallbackTrustManager.chain(SslUtils.createDefaultTrustManager(), vphTrust);
    final SSLContext ssl = SSLContext.getInstance("TLS");
    ssl.init(null, new TrustManager[]{chained}, null);
    HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
    final HostnameVerifier verifier = AllowTrustedVerifier.accept(vphTrust.getAcceptedIssuers());
    HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    attemptConnection(GOOGLE);
    attemptConnection(SHEFFIELD);
  }

  private static synchronized void attemptConnection(final URI target) {
    System.out.println("Connecting to <" + target +">");
    HttpURLConnection connection = null;
    try {
      final URL remote = target.toURL();
      connection = (HttpURLConnection) remote.openConnection();
      connection.setReadTimeout(500); // milliseconds
      connection.connect();
      try {
        connection.getResponseCode(); // trigger 4xx IO errors
      } catch (IOException ignored) {
      }
      final int code = connection.getResponseCode();
      final String message = connection.getResponseMessage();
      System.out.println("Connected - Response : " + message + "(" + code + ")");
    } catch (IOException e) {
      System.err.println("Connecting to <" + target + "> failed : " + e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

}
