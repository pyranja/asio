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

import at.ac.univie.isc.asio.database.Jdbc;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OverrideJdbcConfigTest {

  @Test
  public void should_keep_dataset() throws Exception {
    final OverrideJdbcConfig subject = new OverrideJdbcConfig(new Jdbc());
    final NestConfig initial = NestConfig.empty();
    final NestConfig processed = subject.apply(initial);
    assertThat(processed.getDataset(), sameInstance(initial.getDataset()));
  }

  @Test
  public void should_replace_jdbc_connection_props_with_global() throws Exception {
    final Map<String, String> props = new HashMap<>();
    props.put("key", "value");
    final Jdbc override = new Jdbc()
        .setUrl("jdbc:asio:override")
        .setDriver("MyJdbcDriver")
        .setUsername("override-username")
        .setPassword("override-password")
        .setProperties(props);
    final OverrideJdbcConfig subject = new OverrideJdbcConfig(override);
    final Jdbc processed = subject.apply(NestConfig.empty()).getJdbc();
    assertThat(processed.getUrl(), equalTo("jdbc:asio:override"));
    assertThat(processed.getDriver(), equalTo("MyJdbcDriver"));
    assertThat(processed.getUsername(), equalTo("override-username"));
    assertThat(processed.getPassword(), equalTo("override-password"));
    assertThat(processed.getProperties(), equalTo(props));
  }

  @Test
  public void should_not_use_the_mutable_global_instance_as_override() throws Exception {
    final Jdbc override = new Jdbc();
    final Jdbc processed = new OverrideJdbcConfig(override).apply(NestConfig.empty()).getJdbc();
    assertThat(processed, not(sameInstance(override)));
  }

  @Test
  public void should_not_override_schema() throws Exception {
    final Jdbc override = new Jdbc().setSchema("override");
    final NestConfig initial = NestConfig.empty();
    initial.getJdbc().setSchema("original");
    final NestConfig processed = new OverrideJdbcConfig(override).apply(initial);
    assertThat(processed.getJdbc().getSchema(), equalTo("original"));
  }
}
