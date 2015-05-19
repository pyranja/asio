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
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Category(Integration.class)
public class FeatureWhoami extends IntegrationTest {
  // @formatter:off

  @Test
  public void delivers_json() {
    given()
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/json"))
    ;
  }

  @Test
  public void contains_delegated_credentials() {
    given().delegate("user", "password")
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("name", Matchers.is("user"))
      .body("secret", Matchers.is("password"))
    ;
  }

  @Test
  public void has_no_identity_if_no_delegated_credentials_present() {
    given()
      .and()
      .header(HttpHeaders.ACCEPT, "application/json")
    .when()
      .get("whoami")
    .then()
      .body("name", is(nullValue()))
      .body("secret", is(nullValue()))
    ;
  }
}
