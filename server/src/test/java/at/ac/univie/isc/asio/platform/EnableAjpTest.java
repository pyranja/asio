/*
 * #%L
 * asio server
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
package at.ac.univie.isc.asio.platform;

import at.ac.univie.isc.asio.io.Payload;
import org.apache.tomcat.ajp.AjpClient;
import org.apache.tomcat.ajp.ClientMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

public class EnableAjpTest {
  public static void main(String[] args) throws IOException {
    try (final AjpClient client = new AjpClient(InetAddress.getByName("localhost"), 8009).connect()) {
      final ClientMessage cpong = client.cping();
      System.out.println(Payload.decodeUtf8(cpong.getBuffer()));

      final ClientMessage response = client.get(URI.create("/api/whoami"));
      System.out.println(response);
      System.out.println(Payload.decodeUtf8(response.getBuffer()));
    }
  }
}
