/*
 * #%L
 * asio ssl-fix
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
