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
package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.database.Jdbc;
import com.zaxxer.hikari.HikariConfig;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class JdbcToolsTest {

  public static class IsValidJdbcUrl {
    @Test
    public void should_accept_mysql_jdbc_url() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl("jdbc:mysql://localhost/test"), equalTo(true));
    }

    @Test
    public void should_reject_null_as_jdbc_url() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl(null), equalTo(false));
    }

    @Test
    public void should_reject_url_if_jdbc_prefix_missing() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl("some-text"), equalTo(false));
    }

    @Test
    public void should_accept_h2_jdbc_url() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl("jdbc:h2:mem:test"), equalTo(true));
    }
  }


  public static class InferJdbcDriver {
    @Test
    public void should_return_absent_if_driver_unknown() throws Exception {
      assertThat(JdbcTools.inferDriverClass("jdbc:test://localhost/").isPresent(), equalTo(false));
    }

    @Test
    public void should_find_mysql_driver() throws Exception {
      assertThat(JdbcTools.inferDriverClass("jdbc:mysql://localhost/").get(),
          equalTo("com.mysql.jdbc.Driver"));
    }

    @Test
    public void should_find_h2_driver() throws Exception {
      assertThat(JdbcTools.inferDriverClass("jdbc:h2:mem").get(),
          equalTo("org.h2.Driver"));
    }

    @Test
    public void should_return_absent_if_url_null() throws Exception {
      assertThat(JdbcTools.inferDriverClass(null).isPresent(), equalTo(false));
    }
  }

  @RunWith(Parameterized.class)
  public static class InferSchema {
    @Parameterized.Parameters(name = "{index} : {0} -> {1}")
    public static Iterable<Object[]> urls() {
      // { <jdbc-url>, <expected-schema> }
      return Arrays.asList(new Object[][] {
          { "jdbc:mysql:///database", "database" }
          , { "jdbc:mysql://localhost:9090/database", "database" }
          , { "jdbc:mysql:///database?key=value", "database" }
          , { "jdbc:mysql://localhost:9090/database?key=value", "database" }
          , { "jdbc:mysql://120.39.45.12/database", "database" }
          , { "jdbc:mysql://120.39.45.12/database?key=value&other=value", "database" }
          , { "jdbc:mysql://120.39.45.12/database?key=value?other=value", "database" }
          , { "jdbc:mysql://database", null }
          , { "jdbc:mysql://localhost:8080", null }
          , { "jdbc:mysql", null }
          , { "jdbc:mysql://", null }
          , { "jdbc:mysql://123?key=value/database", "database" } // malformed jdbc url - will fail on connect
          , { "jdbc:mysql:///", null }
          , { "jdbc:mysql:///?key=value", null }
          , { "jdbc:mysql://localhost/?key=value", null }
          , { "jdbc:h2:mem", null }
          , { "gaga1234", null }
          , { null, null }
      });
    }

    @Parameterized.Parameter(0)
    public String jdbcUrl;
    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void should_find_expected_schema() throws Exception {
      assertThat(JdbcTools.inferSchema(jdbcUrl).orNull(), equalTo(expected));
    }
  }

  public static class PopulateSettings {
    @Rule
    public final ExpectedException error = ExpectedException.none();
    private final Jdbc jdbc = new Jdbc().setUrl("jdbc:test:///");

    @Test
    public void should_transfer_jdbc_settings() throws Exception {
      jdbc
          .setUrl("jdbc:mysql:///")
          .setUsername("username")
          .setPassword("password")
          .setSchema("schema")
          .setProperties(Collections.singletonMap("key", "value"));
      final HikariConfig hikari = JdbcTools.populate(new HikariConfig(), "test", jdbc);
      assertThat(hikari.getJdbcUrl(), equalTo("jdbc:mysql:///"));
      assertThat(hikari.getUsername(), equalTo("username"));
      assertThat(hikari.getPassword(), equalTo("password"));
      assertThat(hikari.getCatalog(), equalTo("schema"));
      assertThat(hikari.getDataSourceProperties(), Matchers.<Object, Object>hasEntry("key", "value"));
    }

    @Test
    public void should_eagerly_load_driver_class() throws Exception {
      error.expect(RuntimeException.class);
      error.expectCause(Matchers.<Throwable>instanceOf(ClassNotFoundException.class));
      error.expectMessage("'not-existing' could not be loaded");
      JdbcTools.populate(new HikariConfig(), "test", jdbc.setDriver("not-existing"));
    }

    @Test
    public void should_use_id_in_pool_and_thread_names() throws Exception {
      final HikariConfig hikari = JdbcTools.populate(new HikariConfig(), "test", jdbc);
      assertThat(hikari.getPoolName(), containsString("test"));
      final Thread thread = hikari.getThreadFactory().newThread(new Runnable() {
        @Override
        public void run() { /* no-op */ }
      });
      assertThat(thread.getName(), containsString("test"));
    }
  }


  public static class InjectRequiredJdbcProperties {
    @Test
    public void should_force_h2_properties() throws Exception {
      final Map<String, String> original = new HashMap<>();
      original.put("MODE", "wrong");
      original.put("DATABASE_TO_UPPER", "true");
      final Properties injected = JdbcTools.injectRequiredProperties(original, "jdbc:h2:mem");
      assertThat(injected, has("MODE", "MYSQL"));
      assertThat(injected, has("DATABASE_TO_UPPER", "false"));
    }

    private Matcher<? super Map<?, ?>> has(final String key, final String value) {
      return Matchers.<Object, Object>hasEntry(key, value);
    }
  }
}
