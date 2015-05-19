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

import at.ac.univie.isc.asio.tool.Timeout;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class DefaultTimeoutTest {
  static final Timeout FALLBACK = Timeout.from(30, TimeUnit.SECONDS);
  private final NestConfig config = NestConfig.empty();

  private final DefaultTimeout subject = new DefaultTimeout(FALLBACK);

  @Test
  public void should_keep_configured_explicit_timeout() throws Exception {
    final Timeout configured = Timeout.from(100, TimeUnit.MILLISECONDS);
    assert !configured.equals(FALLBACK) : "illegal test definition";
    config.getDataset().setTimeout(configured);
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(configured));
  }

  @Test
  public void should_override_undefined_timeout() throws Exception {
    config.getDataset().setTimeout(Timeout.undefined());
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(FALLBACK));
  }

  @Test
  public void should_override_missing_timeout() throws Exception {
    config.getDataset().setTimeout(null);
    subject.apply(config);
    assertThat(config.getDataset().getTimeout(), equalTo(FALLBACK));
  }
}
