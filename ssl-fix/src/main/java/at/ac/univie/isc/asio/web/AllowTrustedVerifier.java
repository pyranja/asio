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
