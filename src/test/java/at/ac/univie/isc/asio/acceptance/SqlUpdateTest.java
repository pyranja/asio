package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.engine.sql.Head;
import at.ac.univie.isc.asio.engine.sql.SqlResult;
import at.ac.univie.isc.asio.engine.sql.Update;
import at.ac.univie.isc.asio.sql.H2Provider;
import at.ac.univie.isc.asio.sql.KeyedRow;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.compatibleTo;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static javax.ws.rs.core.Response.Status.Family.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class SqlUpdateTest extends AcceptanceHarness {

  private static final String APPLICATION_SQL_UPDATE = "application/sql-update";

  // sql
  private static final String MOD_TABLE = "PATIENT";
  private static final String INSERT = "INSERT INTO " + MOD_TABLE + " VALUES(42, 'test-name')";

  public static final SqlResult EXPECTED_RESULT = new SqlResult()
      .withHead(new Head().withStatement(INSERT))
      .withUpdate(new Update().withAffected(1));

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.FULL_ACCESS.resolve("sql");
  }

  @Before
  public void setUp() throws SQLException {
    clear();
  }

  @After
  public void tearDown() throws SQLException {
    clear();
  }

  @Test
  public void insert_as_form_param() throws Exception {
    final Form values = new Form();
    values.param(PARAM_UPDATE, INSERT);
    response = client.accept(XML).form(values);
    verifyResponse();
    wasInserted();
  }

  @Test
  public void insert_as_payload() throws Exception {
    response = client.accept(XML).type(APPLICATION_SQL_UPDATE).post(INSERT);
    verifyResponse();
    wasInserted();
  }

  @Test
  public void bad_query_parameter() throws Exception {
    final Form values = new Form();
    values.param(PARAM_UPDATE, "");
    response = client.accept(XML).post(values);
    assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
  }

  @Test
  public void unacceptable_media_type() throws Exception {
    response =
        client.accept(MediaType.valueOf("test/notexisting")).type(APPLICATION_SQL_UPDATE)
            .post("test-update");
    assertEquals(CLIENT_ERROR, familyOf(response.getStatus()));
  }

  private void verifyResponse() {
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(XML)));
    final SqlResult result =
        JAXB.unmarshal(response.readEntity(InputStream.class), SqlResult.class);
    assertThat(result, is(EXPECTED_RESULT));
  }

  private void wasInserted() {
    final Map<String, KeyedRow> expected =
        ImmutableMap.of("42", new KeyedRow("42", Arrays.asList("42", "test-name")));
    final Map<String, KeyedRow> actual =
        ReferenceQuery.forQuery("SELECT * FROM " + MOD_TABLE, "ID").getReference();
    assertEquals(expected, actual);
  }

  // clear the update test integration table
  void clear() throws SQLException {
    try (Connection conn = H2Provider.connect()) {
      final Statement stmt = conn.createStatement();
      stmt.execute("DELETE FROM " + MOD_TABLE);
      stmt.close();
    }
  }
}
