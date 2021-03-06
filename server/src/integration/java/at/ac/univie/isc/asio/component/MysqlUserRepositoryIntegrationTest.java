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
package at.ac.univie.isc.asio.component;

import at.ac.univie.isc.asio.Integration;
import at.ac.univie.isc.asio.database.MysqlUserRepository;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.sql.Database;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.ac.univie.isc.asio.tool.Pretty.substitute;
import static com.google.common.base.Predicates.containsPattern;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

@Category(Integration.class)
public class MysqlUserRepositoryIntegrationTest {
  private static final Database ROOT = Database.create("jdbc:mysql:///?allowMultiQueries=true")
      .credentials("root", "change")
      .build();
  public static final String TEST_SCHEMA_NAME = "test_authorization";
  public static final Collection<String> TEST_PRIVILEGES = Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE");

  @BeforeClass
  public static void ensureMysqlAvailable() {
    assumeTrue("external test database is not available", ROOT.isAvailable());
    ROOT.execute(Classpath.read("database/MysqlUserRepositoryTest-schema.sql"));
  }

  @AfterClass
  public static void cleanUpDatabase() {
    if (ROOT.isAvailable()) {
      ROOT.execute(Classpath.read("database/MysqlUserRepositoryTest-cleanup.sql"));
    }
  }

  private final Database db = Database.create("jdbc:mysql:///test_authorization")
      .credentials("authorizer", "authorizer").build();
  private final MysqlUserRepository subject = new MysqlUserRepository(db.datasource(), TEST_PRIVILEGES);

  @Before
  public void tearDown() throws Exception {
    subject.dropUserOf(TEST_SCHEMA_NAME);
  }

  @Test
  public void can_connect_to_test_database() throws Exception {
    final Table<Integer, String, String> results = db.reference("SELECT DATABASE() AS `schema`");
    assertThat(results.get(0, "schema"), is(TEST_SCHEMA_NAME));
  }

  @Test
  public void should_create_user_for_database() throws Exception {
    final Identity credentials = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(usersWith(credentials), hasSize(1));
  }

  @Test
  public void should_yield_same_credentials_if_schema_name_is_same() throws Exception {
    final Identity first = subject.createUserFor(TEST_SCHEMA_NAME);
    final Identity second = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(first, is(second));
  }

  @Test
  public void should_not_use_schema_name_as_password() throws Exception {
    final Identity credentials = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(credentials.getSecret(), is(not(equalToIgnoringCase(TEST_SCHEMA_NAME))));
  }

  @Test
  public void should_use_a_long_password() throws Exception {
    final Identity credentials = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(credentials.getSecret().length(), greaterThan(10));
  }

  @Test
  public void should_assign_privileges_on_given_schema_only() throws Exception {
    final Identity user = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(findSingleGrantFor(user), matchesPattern(
        "(?i)^GRANT .* ON `" + TEST_SCHEMA_NAME + "`\\.\\* TO .*$"));
  }

  @Test
  public void should_assign_read_privilege() throws Exception {
    final Identity user = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(findPrivilegesOf(user), hasItem("SELECT"));
  }

  @Test
  public void should_assign_write_privilege() throws Exception {
    final Identity user = subject.createUserFor(TEST_SCHEMA_NAME);
    assertThat(findPrivilegesOf(user), hasItems("INSERT", "UPDATE", "DELETE"));
  }

  @Test
  public void should_assign_custom_privileges() throws Exception {
    final MysqlUserRepository custom =
        new MysqlUserRepository(db.datasource(), Arrays.asList("DROP"));
    final Identity user = custom.createUserFor(TEST_SCHEMA_NAME);
    assertThat(findPrivilegesOf(user), contains("DROP"));
  }

  @Test
  public void should_remove_user() throws Exception {
    final Identity credentials = subject.createUserFor(TEST_SCHEMA_NAME);
    subject.dropUserOf(TEST_SCHEMA_NAME);
    assertThat(usersWith(credentials), is(empty()));
  }

  @Test
  public void should_fail_silently_if_user_does_not_exists() throws Exception {
    subject.dropUserOf(TEST_SCHEMA_NAME);
  }

  private List<String> findPrivilegesOf(final Identity user) {
    final String grant = findSingleGrantFor(user);
    assertThat(grant, matchesPattern(Pattern.compile("(?i)^GRANT\\b(.*)\\bON .*$")));
    final Matcher matcher = Pattern.compile("(?i)^GRANT\\b(.*)\\bON .*$").matcher(grant);
    matcher.matches();
    final String[] privileges =
        matcher.group(1).replaceAll("\\s", "").toUpperCase(Locale.ENGLISH).split(",");
    return Arrays.asList(privileges);
  }

  private String findSingleGrantFor(final Identity credentials) {
    final String query = "SHOW GRANTS FOR `${username}`@localhost";
    final Map<String, String> bindings =
        Collections.singletonMap("username", credentials.getName());
    final Table<Integer, String, String> permissions = ROOT.reference(substitute(query, bindings));
    // filter out the USAGE (=> implies no privileges) entry
    return getOnlyElement(filter(permissions.values(), Predicates.not(containsPattern("(?i)^GRANT USAGE ON *.* TO .*$"))));
  }

  private Collection<String> usersWith(final Identity credentials) {
    final String query =
        "SELECT User FROM mysql.user WHERE User = '${username}' AND Password = PASSWORD('${password}')";
    final Map<String, String> bindings =
        ImmutableMap.of("username", credentials.getName(), "password", credentials.getSecret());
    final Table<Integer, String, String> table = ROOT.reference(substitute(query, bindings));
    return table.column("User").values();
  }
}
