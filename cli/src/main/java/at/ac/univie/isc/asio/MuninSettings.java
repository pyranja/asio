package at.ac.univie.isc.asio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * Configuration attributes of the munin client application.
 */
@ConfigurationProperties("munin")
public class MuninSettings {

  /**
   * http endpoint of the asio server.
   */
  @NotNull
  public URI serverAddress;

  /**
   * if set https connections do not verify authenticity of hostname and server certificate.
   * (default = false)
   */
  public boolean insecureConnection = false;

  /**
   * basic auth username.
   */
  @NotNull
  public String username;

  /**
   * basic auth password.
   */
  @NotNull
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
