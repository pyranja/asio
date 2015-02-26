package at.ac.univie.isc.asio;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global asio settings.
 */
@ConfigurationProperties("asio")
public class AsioSettings {

  /**
   * BasicAuth password of role-based accounts.
   */
  @NotEmpty
  public String secret;

  /**
   * default timeout value in milliseconds. (default = 5000ms)
   */
  public long timeout = 5_000;

  @Override
  public String toString() {
    return "AsioSettings{" +
        "secret='" + secret + '\'' +
        ", timeout=" + timeout +
        '}';
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(final String secret) {
    this.secret = secret;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(final long timeout) {
    this.timeout = timeout;
  }
}
