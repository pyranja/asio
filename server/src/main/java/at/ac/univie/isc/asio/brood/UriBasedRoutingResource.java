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

import at.ac.univie.isc.asio.AsioFeatures;
import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Dataset;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.engine.DatasetResource;
import at.ac.univie.isc.asio.security.WhoamiResource;
import at.ac.univie.isc.asio.spring.Holder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;

/**
 * Add the ignored authority element to the dataset routing path.
 */
@Brood
@Primary
@ConditionalOnProperty(AsioFeatures.VPH_URI_AUTH)
@Path("/{target}/{authority}")
public class UriBasedRoutingResource extends DefaultRoutingResource {
  @Autowired
  public UriBasedRoutingResource(final DatasetResource dataset,
                                 final WhoamiResource whoami,
                                 final Holder<Dataset> activeDataset) {
    super(dataset, whoami, activeDataset);
  }

  @PostConstruct
  @Override
  void report() {
    log.info(Scope.SYSTEM.marker(), "vph-uri-auth routing activated");
  }
}
