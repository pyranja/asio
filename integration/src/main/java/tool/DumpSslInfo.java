/*
 * #%L
 * asio integration
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
