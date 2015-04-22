package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.database.Jdbc;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * Global asio settings.
 */
@ConfigurationProperties("asio")
public class AsioSettings {

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
  public AsioApi api = new AsioApi();

  @NestedConfigurationProperty
  @NotNull
  public AsioFeatures feature = new AsioFeatures();

  /** optional */
  @NestedConfigurationProperty
  public Jdbc jdbc;

  @Override
  public String toString() {
    return "AsioSettings{" +
        "timeout=" + timeout +
        ", home='" + home + '\'' +
        ", metadataRepository=" + metadataRepository +
        System.lineSeparator() + ", api=" + api +
        System.lineSeparator() + ", feature=" + feature +
        System.lineSeparator() + ", jdbc=" + jdbc +
        System.lineSeparator() + '}';
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

  public AsioApi getApi() {
    return api;
  }

  public void setApi(final AsioApi api) {
    this.api = api;
  }

  public AsioFeatures getFeature() {
    return feature;
  }

  public void setFeature(final AsioFeatures feature) {
    this.feature = feature;
  }

  public Jdbc getJdbc() {
    return jdbc;
  }

  public void setJdbc(final Jdbc jdbc) {
    this.jdbc = jdbc;
  }
}
