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
