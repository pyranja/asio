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
package at.ac.univie.isc.asio.flock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.net.URI;

@ConfigurationProperties("flock")
class FlockSettings {
  /**
   * Global identifier of the flock service. (default = 'asio:///flock/')
   */
  @NotNull
  public URI identifier = URI.create("asio:///flock/");

  @Override
  public String toString() {
    return "FlockSettings{" +
        "identifier=" + identifier +
        '}';
  }

  public URI getIdentifier() {
    return identifier;
  }

  public void setIdentifier(final URI identifier) {
    this.identifier = identifier;
  }
}
