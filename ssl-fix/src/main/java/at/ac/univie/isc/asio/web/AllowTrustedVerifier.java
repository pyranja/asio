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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

/**
 * Allows hostname mismatches in known, trusted certificates.
 */
public class AllowTrustedVerifier implements HostnameVerifier {
  public static HostnameVerifier accept(final X509Certificate... certificates) {
    return new AllowTrustedVerifier(certificates);
  }

  private final Set<X509Certificate> trusted;

  private AllowTrustedVerifier(final X509Certificate[] certificates) {
    assert certificates != null : "got null instead of trusted certificates";
    this.trusted = new HashSet<>(Arrays.asList(certificates));
  }

  /**
   * Reject unless the peer presented a trusted certificate.
   * @param hostname not used
   * @param session of ssl connection with certificate chain
   * @return true if peer is trusted, false else
   */
  @Override
  public boolean verify(final String hostname, final SSLSession session) {
    final Certificate[] chain = certificateChainOrNullFrom(session);
    final X509Certificate peerCertificate = findLeaf(chain);
    return peerCertificate != null && trusted.contains(peerCertificate);
  }

  /**
   * @param session of SSL connection
   * @return chain of peer certificates if existing or null
   */
  private Certificate[] certificateChainOrNullFrom(final SSLSession session) {
    Certificate[] chain;
    try {
      chain = session.getPeerCertificates();
    } catch (SSLPeerUnverifiedException ignored) {
      chain = null;
    }
    return chain;
  }

  /**
   * @param chain ordered from leaf to root certificate
   * @return the first certificate in the chain or null if chain is invalid
   */
  private X509Certificate findLeaf(final Certificate[] chain) {
    if (chain != null && chain.length > 0) {
      if (chain[0] instanceof X509Certificate) {
        return (X509Certificate) chain[0];
      }
    }
    return null;
  }
}
