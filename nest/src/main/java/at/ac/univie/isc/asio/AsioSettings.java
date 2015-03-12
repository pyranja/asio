package at.ac.univie.isc.asio;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.constraints.NotNull;

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

  @NestedConfigurationProperty
  @NotNull
  public AsioFeatures feature;

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

  public AsioFeatures getFeature() {
    return feature;
  }

  public void setFeature(final AsioFeatures feature) {
    this.feature = feature;
  }
}
