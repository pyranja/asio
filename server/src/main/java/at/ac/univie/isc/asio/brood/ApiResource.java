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

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.insight.EventResource;
import at.ac.univie.isc.asio.security.WhoamiResource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Path;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Entry point to the management api resources. Mainly to provide a common prefix.
 */
@Brood
@Path("/api")
public class ApiResource {
  private static final Logger log = getLogger(DefaultRoutingResource.class);

  private final ContainerResource container;
  private final WhoamiResource whoami;
  private final EventResource events;

  @Autowired
  public ApiResource(final ContainerResource container, final WhoamiResource whoami, final EventResource events) {
    this.container = container;
    this.whoami = whoami;
    this.events = events;
    log.info(Scope.SYSTEM.marker(), "active");
  }

  @Path("/container")
  public ContainerResource forwardToContainer() {
    return container;
  }

  @Path("/whoami")
  public WhoamiResource forwardToWhoami() {
    return whoami;
  }

  @Path("/events")
  public EventResource forwardToEvents() {
    return events;
  }
}
