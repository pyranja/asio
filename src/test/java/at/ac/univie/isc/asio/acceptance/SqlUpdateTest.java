package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.Head;
import at.ac.univie.isc.asio.SqlResult;
import at.ac.univie.isc.asio.Update;
import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.compatibleTo;
import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasFamily;
import static javax.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class SqlUpdateTest extends AcceptanceHarness {
  private static final String MOD_TABLE = "PATIENT";
  private static final String INSERT_SQL_COMMAND =
      "INSERT INTO " + MOD_TABLE + " VALUES(42, 'test-name')";

  @Override
  protected URI getTargetUrl() {
    return fullAccess().resolve("sql");
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
    final Form values = new Form().param("update", INSERT_SQL_COMMAND);
    response = client().request(Mime.XML.type()).post(Entity.form(values));
    verifyResponse();
    verifyInsertion();
  }

  @Test
  public void insert_as_payload() throws Exception {
    response = client().request(Mime.XML.type()).post(Entity.entity(INSERT_SQL_COMMAND, Mime.UPDATE_SQL.type()));
    verifyResponse();
    verifyInsertion();
  }

  @Test
  public void bad_form_parameter() throws Exception {
    final Form values = new Form().param("update", "");
    response = client().request().post(Entity.form(values));
    assertThat(response, hasFamily(CLIENT_ERROR));
  }

  @Test
  public void bad_payload() throws Exception {
    response = client().request().post(Entity.entity("", Mime.UPDATE_SQL.type()));
    assertThat(response, hasFamily(CLIENT_ERROR));
  }

  @Test
  public void unacceptable_media_type() throws Exception {
    response = client().request(MediaType.valueOf("test/not-existing"))
        .post(Entity.entity("ignored", Mime.UPDATE_SQL.type()));
    assertThat(response, hasFamily(CLIENT_ERROR));
  }

  private void verifyResponse() {
    assertThat(response, hasFamily(SUCCESSFUL));
    assertThat(response.getMediaType(), is(compatibleTo(Mime.XML.type())));
    final SqlResult result =
        JAXB.unmarshal(response.readEntity(InputStream.class), SqlResult.class);
    final SqlResult expected = new SqlResult()
        .withHead(new Head().withStatement(INSERT_SQL_COMMAND))
        .withUpdate(new Update().withAffected(1));
    assertThat(result, is(expected));
  }

  private void verifyInsertion() {
    final Table<Integer, String, String> expected =
        ImmutableTable.<Integer, String, String>builder()
            .put(0, "ID", "42")
            .put(0, "NAME", "test-name")
            .build();
    final Table<Integer, String, String> actual =
        database().reference("SELECT * FROM " + MOD_TABLE);
    assertThat(actual, is(expected));
  }

  // clear the update test integration table
  void clear() throws SQLException {
    database().execute("DELETE FROM " + MOD_TABLE);
  }
}
