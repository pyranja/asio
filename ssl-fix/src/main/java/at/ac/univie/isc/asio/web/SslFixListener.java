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

import sun.net.www.protocol.https.DefaultHostnameVerifier;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Customize default SSL components to enable SSL connections to VPH SecProxies.
 * <p><strong>SECURITY WARNING</strong></p>
 * <p>
 *   Deploying this listener will modify the default SSL configurations to trust all certificates
 *   from the bundled {@link #VPH_STORE_PATH truststore} unconditionally and disable hostname
 *   verification if the remote peer can present one of these certificates.
 * </p>
 */
@WebListener
public class SslFixListener implements ServletContextListener {
  public static final String VPH_STORE_PASSWORD = "changeit";
  public static final String VPH_STORE_PATH = "/vph-truststore";

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    final KeyStore vphStore = SslUtils.loadKeystore(VPH_STORE_PATH, VPH_STORE_PASSWORD.toCharArray());
    final X509TrustManager vph = SslUtils.createTrustManagerFrom(vphStore);
    final SSLContext ssl = createSslContextWith(vph);
    HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
    final HostnameVerifier verifier = AllowTrustedVerifier.accept(vph.getAcceptedIssuers());
    HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    sce.getServletContext().log("SSL fix installed");
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    HttpsURLConnection.setDefaultHostnameVerifier(new DefaultHostnameVerifier());
    HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
    sce.getServletContext().log("SSL fix removed");
  }

  private SSLContext createSslContextWith(final X509TrustManager vph) {
    final X509TrustManager system = SslUtils.createDefaultTrustManager();
    final TrustManager chained = FallbackTrustManager.chain(system, vph);
    try {
      final SSLContext ssl = SSLContext.getInstance("TLS");
      ssl.init(null, new TrustManager[] { chained }, null);
      return ssl;
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new SslUtils.InitializationFailure(e);
    }
  }
}
