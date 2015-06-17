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

import at.ac.univie.isc.asio.Dataset;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.spring.Holder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Route requests to the correct dataset resource.
 */
@Component
@Path("/{target}")
public class DefaultRoutingResource extends BaseContainerRegistry {
  private final DatasetResource dataset;
  private final WhoamiResource whoami;

  private final Holder<Dataset> activeDataset;

  @Autowired
  public DefaultRoutingResource(final DatasetResource dataset,
                                final WhoamiResource whoami,
                                final Holder<Dataset> activeDataset) {
    this.dataset = dataset;
    this.whoami = whoami;
    this.activeDataset = activeDataset;
  }

  @PostConstruct
  void report() {
    log.info(Scope.SYSTEM.marker(), "default router loaded");
  }

  @Path("/")
  public DatasetResource forwardToDataset(@PathParam("target") final Id target) {
    activeDataset.set(find(target));
    return dataset;
  }

  @Path("/whoami")
  public WhoamiResource forwardToDatasetWhoami() {
    return whoami;
  }
}
