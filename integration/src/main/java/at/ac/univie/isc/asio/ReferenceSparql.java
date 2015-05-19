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
import at.ac.univie.isc.asio.io.Classpath;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.*;
import static org.hamcrest.CoreMatchers.is;

@Category(Integration.class)
@RunWith(Parameterized.class)
public class ReferenceSparql extends IntegrationTest {

  @Parameterized.Parameters(name = "case {index} : {0}")
  public static Iterable<Object[]> cases() {
    return Arrays.asList(new Object[][] {
        { "select_single_person_id" }
        , { "select_all_persons" }
        , { "datetime_using_exact_filter" }
        , { "datetime_using_less_than_filter" }
    });
  }

  @Parameterized.Parameter(0)
  public String testCase;

  @Before
  public void prepareDatabase() {
    ensureDatabaseAccessible();
    database().execute(Classpath.read("sql/database.integration.sql"));
  }

  /** read the query template for current test case */
  private String query() throws IOException {
    return Classpath.read(Pretty.format("sparql/reference/%s.rq", testCase));
  }

  /** read the expected results for current test case */
  private ResultSet expected() throws IOException {
    return ResultSetFactory.fromXML(Classpath.fetch(Pretty.format("sparql/reference/%s.srx", testCase)));
  }

  // @formatter:off

  @Test
  public void query_yields_expected_xml_result() throws Exception {
    given().role("read").and()
      .header(org.apache.http.HttpHeaders.ACCEPT, "application/xml")
      .formParam("query", query())
    .when()
      .post("/sparql")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/sparql-results+xml"))
      .body(is(sparqlXmlEqualTo(expected())));
  }

  @Test
  public void query_yields_expected_json_result() throws Exception {
    given().role("read").and()
      .header(org.apache.http.HttpHeaders.ACCEPT, "application/json")
      .formParam("query", query())
    .when()
      .post("/sparql")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("application/sparql-results+json"))
      .body(is(sparqlJsonEqualTo(expected())));
  }

  @Test
  public void query_yields_expected_csv_result() throws Exception {
    given().role("read").and()
      .header(org.apache.http.HttpHeaders.ACCEPT, "text/csv")
      .formParam("query", query())
    .when()
      .post("/sparql")
    .then()
      .statusCode(is(HttpStatus.SC_OK))
      .contentType(compatibleTo("text/csv"))
      .body(is(sparqlCsvEqualTo(expected())));
  }
}
