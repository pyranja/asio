package at.ac.univie.isc.asio.munin;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Properties;

/**
 * Configuration attributes of the munin client application.
 */
class Settings {
  public static Settings fromProperties(final Properties input) {
    final Settings it = new Settings();
    it.setServerAddress(URI.create(input.getProperty("server-address", "server address is missing")));
    it.setUsername(input.getProperty("username"));
    it.setPassword(input.getProperty("password"));
    it.setInsecureConnection(Boolean.parseBoolean(input.getProperty("insecure-connection", "false")));
    return it;
  }

  /**
   * http endpoint of the asio server.
   */
  public URI serverAddress;

  /**
   * if set https connections do not verify authenticity of hostname and server certificate.
   * (default = false)
   */
  public boolean insecureConnection = false;

  /**
   * basic auth username.
   */
  public String username;

  /**
   * basic auth password.
   */
  public String password;

  @Override
  public String toString() {
    return "MuninSettings{" +
        "serverAddress=" + serverAddress +
        ", insecureConnection=" + insecureConnection +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        '}';
  }

  @Nonnull
  public URI getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(@Nonnull final URI serverAddress) {
    this.serverAddress = serverAddress;
  }

  public boolean isInsecureConnection() {
    return insecureConnection;
  }

  public void setInsecureConnection(final boolean insecureConnection) {
    this.insecureConnection = insecureConnection;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }
}
