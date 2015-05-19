/*
 * #%L
 * asio integration
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
package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalToIgnoringCase;

@RunWith(Parameterized.class)
@Category(Integration.class)
public class FeatureRoles extends IntegrationTest {

  @Parameterized.Parameters(name = "{index}: using role {0} for login")
  public static Iterable<Object[]> roles() {
    return Arrays.asList(new Object[][] {
        { "read" }
        , { "user" }
        , { "full" }
        , { "owner" }
        , { "admin" }
    });
  }

  @Parameterized.Parameter(0)
  public String role;

  @Before
  public void skipIfNotSecured() {
    ensureSecured();
  }

  // @formatter:off

  @Test
  public void login_is_role_name() {
    given().role(role)
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("login", equalToIgnoringCase(role))
    ;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void authorities_contain_role() {
    given().role(role)
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("authorities", Matchers.hasItem(equalToIgnoringCase("ROLE_" + role)))
    ;
  }
}
