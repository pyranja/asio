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
package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.tool.Timeout;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

public class JdbcSpecTest {

  private JdbcSpec spec;

  @Test
  public void should_build_correct_spec() throws Exception {
    spec = JdbcSpec.connectTo("jdbcUrl").with("jdbcDriver").authenticateAs("user", "password").use(Timeout.from(1, TimeUnit.SECONDS)).complete();
    assertThat(spec.getUrl(), is("jdbcUrl"));
    assertThat(spec.getDriver(), is("jdbcDriver"));
    assertThat(spec.getUsername(), is("user"));
    assertThat(spec.getPassword(), is("password"));
    assertThat(spec.getTimeout(), is(Timeout.from(1L, TimeUnit.SECONDS)));
  }

  @Test
  public void default_settings() throws Exception {
    spec = JdbcSpec.connectTo("jdbcUrl").with("jdbcDriver").complete();
    assertThat(spec.getUrl(), is("jdbcUrl"));
    assertThat(spec.getDriver(), is("jdbcDriver"));
    assertThat(spec.getCredentials(), is(JdbcSpec.ANONYMOUS_ACCESS));
    assertThat(spec.getTimeout(), is(Timeout.undefined()));
  }

  @Test
  public void should_use_empty_password_if_not_authenticated() throws Exception {
    spec = JdbcSpec.connectTo("test-url").with("test-driver").complete();
    assertThat(spec.getPassword(), is(""));
  }

  @Test
  public void should_use_empty_user_if_not_authenticated() throws Exception {
    spec = JdbcSpec.connectTo("test-url").with("test-driver").complete();
    assertThat(spec.getUsername(), isEmptyString());
  }

  @Test
  public void should_use_given_username_and_password() throws Exception {
    spec = JdbcSpec.connectTo("test-url").with("test-driver").authenticateAs("test-user", "test-password").complete();
    assertThat(spec.getUsername(), is("test-user"));
    assertThat(spec.getPassword(), is("test-password"));
  }

  @Test
  public void should_guess_jdbc_driver() throws Exception {
    spec = JdbcSpec.connectTo("jdbc:h2:test").complete();
    assertThat(spec.getDriver(), is("org.h2.Driver"));
  }

  @Test
  public void should_leave_jdbc_url_as_is_if_no_default_options_necessary() throws Exception {
    spec = JdbcSpec.connectTo("jdbc:genericdb://example.com/test").with("genericDriver").complete();
    assertThat(spec.getUrl(), is("jdbc:genericdb://example.com/test"));
  }

  @Test
  public void should_append_mysql_default_options() throws Exception {
    spec = JdbcSpec.connectTo("jdbc:mysql://example.com/test").complete();
    assertThat(spec.getUrl(), is("jdbc:mysql://example.com/test?zeroDateTimeBehavior=convertToNull"));
  }
}
