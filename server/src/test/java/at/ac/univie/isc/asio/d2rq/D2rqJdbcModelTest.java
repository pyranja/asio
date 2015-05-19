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
package at.ac.univie.isc.asio.d2rq;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class D2rqJdbcModelTest {

  private final D2rqJdbcModel subject = new D2rqJdbcModel();
  private final Database db = new Database(ResourceFactory.createResource());

  @Before
  public void injectTestUrlAndDriver() {
    db.setJDBCDSN("jdbc:db:test");
  }

  @Test
  public void should_find_configured_jdbc_url() throws Exception {
    subject.visit(db);
    assertThat(subject.getUrl(), is("jdbc:db:test"));
  }

  @Test(expected = InvalidD2rqConfig.class)
  public void should_fail_if_jdbc_url_missing() throws Exception {
    subject.visit(new Database(ResourceFactory.createResource()));
  }

  @Test
  public void should_find_driver_class_name() throws Exception {
    db.setJDBCDriver("org.example.Driver");
    subject.visit(db);
    assertThat(subject.getDriver(), equalTo("org.example.Driver"));
  }

  @Test
  public void should_leave_driver_class_name_blank_if_missing() throws Exception {
    subject.visit(db);
    assertThat(subject.getDriver(), nullValue());
  }

  @Test
  public void should_find_configured_credentials() throws Exception {
    db.setUsername("test-user");
    db.setPassword("test-password");
    subject.visit(db);
    assertThat(subject.getUsername(), is("test-user"));
    assertThat(subject.getPassword(), is("test-password"));
  }

  @Test
  public void should_use_empty_string_if_credentials_missing() throws Exception {
    subject.visit(db);
    assertThat(subject.getUsername(), isEmptyString());
    assertThat(subject.getPassword(), isEmptyString());
  }

  @Test
  public void should_leave_schema_blank_if_not_specified() throws Exception {
    subject.visit(db);
    assertThat(subject.getSchema(), nullValue());
  }

  @Test
  public void should_find_configured_schema() throws Exception {
    db.setConnectionProperty("schema", "configured");
    subject.visit(db);
    assertThat(subject.getSchema(), equalTo("configured"));
  }

  @Test
  public void should_find_connection_properties() throws Exception {
    db.setConnectionProperty("test-key", "test-value");
    subject.visit(db);
    assertThat(subject.getProperties(), hasEntry("test-key", "test-value"));
  }

  @Test
  public void should_exclude_schema_from_connection_properties() throws Exception {
    db.setConnectionProperty("schema", "exclude-me");
    subject.visit(db);
    assertThat(subject.getProperties(), not(hasEntry("schema", "exclude-me")));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_copy_properties_to_immutable_map() throws Exception {
    subject.visit(db);
    subject.getProperties().put("should", "fail");
  }

  @Test(expected = InvalidD2rqConfig.class)
  public void should_fail_on_multiple_configurations() throws Exception {
    subject.visit(db);
    subject.visit(db);
  }

  @Test(expected = InvalidD2rqConfig.class)
  public void should_fail_on_missing_config() throws Exception {
    D2rqJdbcModel.parse(new Mapping());
  }
}
