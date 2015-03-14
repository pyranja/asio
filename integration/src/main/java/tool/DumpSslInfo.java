package tool;

import javax.net.ssl.SSLServerSocketFactory;

/**
 * Print all strong cipher suites supported by the JSSE implementation available.
 */
public final class DumpSslInfo {

  public static void main(String[] args) throws Exception {
    final SSLServerSocketFactory ssl = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    System.out.println("supported strong cipher suites:");
    for (final String name : ssl.getSupportedCipherSuites()) {
      if (isStrong(name)) {
        System.out.printf("  %s%n", name);
      }
    }
  }

  private static boolean isStrong(final String name) {
    return
        // require TLS and better than SHA-1
        name.startsWith("TLS") && name.matches(".*_SHA(\\d*)")
            // perfect forward secrecy
            && (name.contains("DHE") || name.contains("ECDHE"))
            // exclude cipher without authentication (mitm)
            && !name.contains("anon")
            // exclude weak algorithms
            && !(name.contains("3DES") || name.contains("RC4"))
            // exclude dummy ciphers
            && !(name.equals("TLS_EMPTY_RENEGOTIATION_INFO_SCSV") || name.contains("NULL"))
        ;
  }
}
