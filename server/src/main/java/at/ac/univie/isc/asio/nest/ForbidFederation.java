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

import at.ac.univie.isc.asio.AsioFeatures;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * If enabled, this configurer disables sparql federation on any deployed container.
 */
@Component
@ConditionalOnProperty(name = AsioFeatures.ALLOW_FEDERATION, havingValue = "false", matchIfMissing = true)
final class ForbidFederation implements Configurer {
  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    input.getDataset().setFederationEnabled(false);
    return input;
  }

  @Override
  public String toString() {
    return "ForbidFederation{}";
  }
}
