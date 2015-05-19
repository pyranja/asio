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

import at.ac.univie.isc.asio.InvalidUsage;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class InferJdbcDriverTest {

  private final NestConfig config = NestConfig.empty();
  private final InferJdbcDriver subject = new InferJdbcDriver();

  @Test
  public void should_keep_explicit_jdbc_driver() throws Exception {
    config.getJdbc().setDriver("org.example.Driver");
    subject.apply(config);
    assertThat(config.getJdbc().getDriver(), equalTo("org.example.Driver"));
  }

  @Test
  public void should_set_driver_if_missing_and_able_to_infer_it() throws Exception {
    config.getJdbc().setUrl("jdbc:mysql://localhost/");
    subject.apply(config);
    assertThat(config.getJdbc().getDriver(), equalTo("com.mysql.jdbc.Driver"));
  }

  @Test(expected = InvalidUsage.class)
  public void should_fail_fast_if_driver_unknown() throws Exception {
    config.getJdbc().setUrl("jdbc:unknown:test");
    subject.apply(config);
  }

  @Test(expected = InvalidUsage.class)
  public void should_fail_fast_if_driver_and_url_missing() throws Exception {
    subject.apply(config);
  }
}
