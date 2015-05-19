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
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Perform sql queries via asio and compare results to direct jdbc queries on the backing database.
 */
@Category(Integration.class)
public class ReferenceSql extends IntegrationTest {

  @Before
  public void prepareDatabase() {
    ensureDatabaseAccessible();
    database().execute(Classpath.read("sql/database.integration.sql"));
  }

  // @formatter:off

  public class Schema {
    @Test
    public void includes_patient_table_in_xml() throws Exception {
      given().role("read").and()
        .header(HttpHeaders.ACCEPT, "application/xml")
      .when()
        .get("/meta/schema")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/xml"))
        .root("schema.table.findAll { it.@name == 'patient' }")
        .content("@schema", is("public"))
        .root("schema.table.find { it.@name == 'patient' }.column.find { it.@name == 'id' }")
//      namespace prefix is non-deterministic
        .content("@type", endsWith("long"))
        .content("@sqlType", startsWith("int"))
        .content("@length", is("10"))
        .root("schema.table.find { it.@name == 'patient' }.column.find { it.@name == 'name' }")
//      namespace prefix is non-deterministic
        .content("@type", endsWith("string"))
        .content("@sqlType", is("varchar"))
        .content("@length", is("255"));
    }

    @Test
    public void includes_patient_table_in_json() throws Exception {
      given().role("read").and()
        .header(HttpHeaders.ACCEPT, "application/json")
      .when()
        .get("/meta/schema")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/json"))
        .root("table.findAll { it.name == 'patient' }")
        .content("", hasSize(1))
        .content("schema", contains("public"))
        .root("table.find { it.name == 'patient' }.column.find { it.name == 'id' }")
        .content("type", is("http://www.w3.org/2001/XMLSchema#long"))
        .content("sqlType", startsWith("int"))
        .content("length", is(10))
        .root("table.find { it.name == 'patient' }.column.find { it.name == 'name' }")
        .content("type", is("http://www.w3.org/2001/XMLSchema#string"))
        .content("sqlType", is("varchar"))
        .content("length", is(255))
        ;
    }
  }

  public class Query {
    @Test
    public void select_as_webrowset() throws Exception {
      given().role("read").and()
        .header(HttpHeaders.ACCEPT, "application/xml")
        .param("query", "SELECT * FROM person")
      .when()
        .post("/sql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/webrowset+xml"))
        .content(is(sqlWebrowsetEqualTo(database().reference("SELECT * FROM person"))));
    }

    @Test
    public void select_as_csv() throws Exception {
      given().role("read").and()
        .header(HttpHeaders.ACCEPT, "text/csv")
        .param("query", "SELECT * FROM person")
      .when()
        .post("/sql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("text/csv"))
        .content(is(sqlCsvEqualTo(database().reference("SELECT * FROM person"))));
    }
  }

  public class Update {

    @Test
    public void insert_as_xml() throws Exception {
      given().role("full").and()
        .header(HttpHeaders.ACCEPT, "application/xml")
        .param("update", "INSERT INTO PATIENT VALUES(42, 'test-name')")
      .when()
        .post("/sql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("application/sql-results+xml"))
        .content("sql.head.@statement", is("INSERT INTO PATIENT VALUES(42, 'test-name')"))
        .content("sql.update.@affected", is("1"));
      assertThat(database().reference("SELECT * FROM PATIENT"), is(expectedInsertion()));
    }

    private final Table<Integer, String, String> EXPECTED_CSV_RESPONSE =
        ImmutableTable.<Integer, String, String>builder()
            .put(0, "statement", "insert into patient values(42, 'test-name')")
            .put(0, "affected", "1")
            .build();

    @Test
    public void insert_as_csv() throws Exception {
      given().role("full").and()
        .header(HttpHeaders.ACCEPT, "text/csv")
        .param("update", "INSERT INTO PATIENT VALUES(42, 'test-name')")
      .when()
        .post("/sql")
      .then()
        .statusCode(is(HttpStatus.SC_OK))
        .contentType(compatibleTo("text/csv"))
        .content(is(sqlCsvEqualTo(EXPECTED_CSV_RESPONSE)));
      assertThat(database().reference("SELECT * FROM PATIENT"), is(expectedInsertion()));
    }

    // @formatter:on

    private Matcher<? super Table<Integer, String, String>> expectedInsertion() {
      return new CustomMatcher<Table<Integer, String, String>>("expect inserted row {id=42, name=test-name}") {
        @Override
        public boolean matches(final Object item) {
          final Table<Integer, String, String> result = (Table<Integer, String, String>) item;
          assertThat("unexpected row count", result.rowKeySet(), hasSize(1));
          final Map<String, String> row = result.row(0);
          assertThat("unexpected column count", row.entrySet(), hasSize(2));
          assertThat(row, hasEntry(equalToIgnoringCase("id"), equalTo("42")));
          assertThat(row, hasEntry(equalToIgnoringCase("name"), equalTo("test-name")));
          return true;
        }
      };
    }
  }
}
