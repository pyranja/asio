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

import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.InvalidUsage;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * Prevent assembly of containers with names of system resource paths.
 */
@Component
final class ForbidReservedNames implements Configurer {

  /**
   * Thrown if a container with a reserved name is assembled.
   */
  public static class IllegalContainerName extends InvalidUsage {
    public IllegalContainerName(final Id illegal) {
      super("'" + illegal + "' is a reserved name");
    }
  }

  private final Set<Id> reserved;

  @Autowired
  ForbidReservedNames(final AsioSettings config) {
    this(config.api.getReservedContainerNames());
  }

  ForbidReservedNames(final Collection<Id> reservedContainerNames) {
    this.reserved = ImmutableSet.copyOf(reservedContainerNames);
  }

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Id name = input.getDataset().getName();
    if (reserved.contains(name)) { throw new IllegalContainerName(name); }
    return input;
  }

  @Override
  public String toString() {
    return "ForbidReservedNames{" +
        "reserved=" + reserved +
        '}';
  }
}
