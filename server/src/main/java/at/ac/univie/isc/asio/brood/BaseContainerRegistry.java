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
package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Skeleton of an event-aware registry service. Internally maps schema names to deployed containers,
 * and keeps the mapping up to date. Container registration and removal is driven by CatalogEvents.
 */
abstract class BaseContainerRegistry {
  /**
   * non-static to allow subclass loggers to be distinguished by name
   */
  protected final Logger log = getLogger(this.getClass());

  protected final ConcurrentMap<Id, Container> registry = new ConcurrentHashMap<>();

  protected Container find(final Id target) {
    final Container found = registry.get(target);
    if (found == null) {
      throw new Id.NotFound(target);
    }
    return found;
  }

  @Subscribe
  public final void onDeploy(final ContainerEvent.Deployed event) {
    log.debug(Scope.SYSTEM.marker(), "registering container <{}>", event.getName());
    final Container former = registry.put(event.getName(), event.getContainer());
    if (former != null) {
      log.warn(Scope.SYSTEM.marker(), "replaced <{}> with <{}> on deployment", former.name(), event.getName());
    }
  }

  @Subscribe
  public final void onDrop(final ContainerEvent.Dropped event) {
    log.debug(Scope.SYSTEM.marker(), "removing container <{}>", event.getName());
    final Container removed = registry.remove(event.getName());
    if (removed == null) {
      log.warn(Scope.SYSTEM.marker(), "dropped container <{}> was not present", event.getName());
    }
  }
}
