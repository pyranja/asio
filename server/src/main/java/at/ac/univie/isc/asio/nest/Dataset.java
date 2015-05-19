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
package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.tool.Timeout;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Objects;

/**
 * Common dataset properties. These are not specific to an engine.
 */
final class Dataset {
  /**
   * Local name of the dataset.
   */
  @NotNull
  private Id name;
  /**
   * Global identifier of the dataset.
   */
  @NotNull
  private URI identifier;
  /**
   * Maximal allowed duration of operations on this dataset. (default: undefined)
   */
  @NotNull
  private Timeout timeout = Timeout.undefined();
  /**
   * Whether federated query processing is supported. (default: false)
   */
  private boolean federationEnabled = false;

  public Id getName() {
    return name;
  }

  public Dataset setName(final Id name) {
    this.name = name;
    return this;
  }

  public URI getIdentifier() {
    return identifier;
  }

  public Dataset setIdentifier(final URI identifier) {
    this.identifier = identifier;
    return this;
  }

  public Timeout getTimeout() {
    return timeout;
  }

  public Dataset setTimeout(final Timeout timeout) {
    this.timeout = timeout;
    return this;
  }

  public boolean isFederationEnabled() {
    return federationEnabled;
  }

  public Dataset setFederationEnabled(final boolean federationEnabled) {
    this.federationEnabled = federationEnabled;
    return this;
  }

  @Override
  public String toString() {
    return "Dataset{" +
        "name=" + name +
        ", identifier='" + identifier + '\'' +
        ", timeout=" + timeout +
        ", federationEnabled=" + federationEnabled +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final Dataset dataset = (Dataset) o;
    return Objects.equals(federationEnabled, dataset.federationEnabled) &&
        Objects.equals(name, dataset.name) &&
        Objects.equals(identifier, dataset.identifier) &&
        Objects.equals(timeout, dataset.timeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, identifier, timeout, federationEnabled);
  }
}
