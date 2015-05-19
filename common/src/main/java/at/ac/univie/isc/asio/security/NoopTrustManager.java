/*
 * #%L
 * asio common
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
