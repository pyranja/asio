/*
 * #%L
 * asio cli
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
