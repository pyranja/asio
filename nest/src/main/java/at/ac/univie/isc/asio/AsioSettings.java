package at.ac.univie.isc.asio;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * Global asio settings.
 */
@ConfigurationProperties("asio")
public class AsioSettings {

  /** the http header used to transmit delegated credentials */
  public static final String DELEGATE_AUTHORIZATION_HEADER = "Delegate-Authorization";
  /**
   * BasicAuth password of role-based accounts.
   */
  @NotEmpty
  public String secret;

  /**
   * default timeout value in milliseconds. (default = 5000ms)
   */
  public long timeout = 5_000;

  /**
   * Path to asio's working directory.
   */
  @NotNull
  public String home;

  /**
   * Http endpoint of the external metadata repository.
   */
  public URI metadataRepository;

  @NestedConfigurationProperty
  @NotNull
  public AsioFeatures feature;

  @Override
  public String toString() {
    return "AsioSettings{" +
        "secret='" + secret + '\'' +
        ", timeout=" + timeout +
        ", home=" + home +
        ", metadataRepository=" + metadataRepository +
        ", feature=" + feature +
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

  public String getHome() {
    return home;
  }

  public void setHome(final String home) {
    this.home = home;
  }

  public URI getMetadataRepository() {
    return metadataRepository;
  }

  public void setMetadataRepository(final URI metadataRepository) {
    this.metadataRepository = metadataRepository;
  }

  public AsioFeatures getFeature() {
    return feature;
  }

  public void setFeature(final AsioFeatures feature) {
    this.feature = feature;
  }
}
