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

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ForbidFederationTest {

  private final ForbidFederation subject = new ForbidFederation();

  @Test
  public void should_keep_federation_disabled() throws Exception {
    final NestConfig config = NestConfig.empty();
    config.getDataset().setFederationEnabled(false);
    assertThat(subject.apply(config).getDataset().isFederationEnabled(), equalTo(false));
  }

  @Test
  public void should_disable_federation_if_enabled() throws Exception {
    final NestConfig config = NestConfig.empty();
    config.getDataset().setFederationEnabled(true);
    assertThat(subject.apply(config).getDataset().isFederationEnabled(), equalTo(false));
  }
}
