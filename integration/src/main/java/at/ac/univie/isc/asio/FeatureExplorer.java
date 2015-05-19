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
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.compatibleTo;
import static org.hamcrest.Matchers.is;

@Category(Integration.class)
public class FeatureExplorer extends IntegrationTest {

  // @formatter:off

  @Test
  public void delivers_sql_browser_page() throws Exception {
    ensureLanguageSupported("sql");
    given().role("read").spec()
    .when()
      .get("explore/sql.html")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/html"));
  }

  @Test
  public void delivers_sparql_browser_page() throws Exception {
    ensureLanguageSupported("sparql");
    given().role("read").spec()
    .when()
      .get("explore/sparql.html")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/html"));
  }

  @Test
  public void delivers_events_page() throws Exception {
    given().role("read").spec()
    .when()
      .get("explore/events.html")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/html"));
  }
}
