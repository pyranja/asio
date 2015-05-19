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
import at.ac.univie.isc.asio.insight.Event;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base for events emitted when modifying the set of active schemas.
 */
public abstract class ContainerEvent extends Event {

  /**
   * Raise whenever a schema is added to the catalog.
   */
  public static final class Deployed extends ContainerEvent {
    public Deployed(final Container container) {
      super("deployed", container);
    }
  }

  /**
   * Raise whenever a schema is removed from the catalog.
   */
  public static final class Dropped extends ContainerEvent {
    public Dropped(final Container container) {
      super("dropped", container);
    }
  }

  // === event implementation ======================================================================

  private final Container container;

  private ContainerEvent(final String subject, final Container container) {
    super("container", subject);
    this.container = container;
  }

  public final Id getName() {
    return container.name();
  }

  @JsonIgnore
  public final Container getContainer() {
    return container;
  }
}
