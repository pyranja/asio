package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.io.Classpath;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static at.ac.univie.isc.asio.matcher.RestAssuredMatchers.*;
import static org.hamcrest.CoreMatchers.is;
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
        .root("schema.table.find { it.@name == 'PATIENT' }")
        .content("@schema", is("PUBLIC"))
        .content("@catalog", is("TEST"))
        .root("schema.table.find { it.@name == 'PATIENT' }.column.find { it.@name == 'ID' }")
//      namespace prefix is non-deterministic
//        .content("@type", is("xsi:long"))
        .content("@sqlType", is("integer"))
        .content("@length", is("10"))
        .root("schema.table.find { it.@name == 'PATIENT' }.column.find { it.@name == 'NAME' }")
//      namespace prefix is non-deterministic
//        .content("@type", is("xsi:string"))
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
        .root("table.find { it.name == 'PATIENT' }")
        .content("schema", is("PUBLIC"))
        .content("catalog", is("TEST"))
        .root("table.find { it.name == 'PATIENT' }.column.find { it.name == 'ID' }")
        .content("type", is("http://www.w3.org/2001/XMLSchema#long"))
        .content("sqlType", is("integer"))
        .content("length", is(10))
        .root("table.find { it.name == 'PATIENT' }.column.find { it.name == 'NAME' }")
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

    private final Table<Integer, String, String> EXPECTED_INSERTION =
        ImmutableTable.<Integer, String, String>builder()
            .put(0, "ID", "42")
            .put(0, "NAME", "test-name")
            .build();

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
      assertThat(database().reference("SELECT * FROM PATIENT"), is(EXPECTED_INSERTION));
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
      assertThat(database().reference("SELECT * FROM PATIENT"), is(EXPECTED_INSERTION));
    }
  }
}
