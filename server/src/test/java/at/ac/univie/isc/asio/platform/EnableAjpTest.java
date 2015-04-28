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
