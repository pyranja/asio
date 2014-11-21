package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.FunctionalTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

import static at.ac.univie.isc.asio.jaxrs.ResponseMatchers.hasStatus;
import static org.junit.Assert.assertThat;

@Category(FunctionalTest.class)
public class PermissionTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return serverAddress();
  }

  @Test
  public void should_fail_on_invalid_permission() throws Exception {
    response = client().path("invalid").request()
        .post(Entity.entity("SELECT 1", Mime.QUERY_SQL.type()));
    assertThat(response, hasStatus(Status.NOT_FOUND));
  }

  @Test
  public void should_deny_query_with_invalid_permission() throws Exception {
    response = client().path("none").path("sql").request()
        .post(Entity.entity("SELECT 1", Mime.QUERY_SQL.type()));
    assertThat(response, hasStatus(Status.FORBIDDEN));
  }

  @Test
  public void should_deny_update_with_read_permission() throws Exception {
    response = client().path("read").path("sql").request()
        .post(Entity.entity("CREATE TABLE nulltable", Mime.UPDATE_SQL.type()));
    assertThat(response, hasStatus(Status.FORBIDDEN));
    // FIXME : assert nulltable has not been created in database
  }

  @Test
  public void should_deny_update_via_get_method() throws Exception {
    response = client().path("full").path("sql")
        .queryParam("update", "CREATE TABLE nulltable").request().get();
    assertThat(response, hasStatus(Status.FORBIDDEN));
  }
}
