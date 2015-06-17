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

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.tool.Timeout;

import java.net.URI;
import java.util.Objects;

/**
 * Configuration properties of a virtual dataset container.
 */
final class FlockConfig {
  private Id name;
  private URI identifier = URI.create("asio:///flock/");
  private Timeout timeout = Timeout.undefined();

  FlockConfig() {}

  public Id getName() {
    return name;
  }

  public FlockConfig setName(final Id name) {
    this.name = name;
    return this;
  }

  public URI getIdentifier() {
    return identifier;
  }

  public FlockConfig setIdentifier(final URI identifier) {
    this.identifier = identifier;
    return this;
  }

  public Timeout getTimeout() {
    return timeout;
  }

  public FlockConfig setTimeout(final Timeout timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public String toString() {
    return "FlockConfig{" +
        "name=" + name +
        ", identifier=" + identifier +
        ", timeout=" + timeout +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final FlockConfig that = (FlockConfig) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(identifier, that.identifier) &&
        Objects.equals(timeout, that.timeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, identifier, timeout);
  }
}
