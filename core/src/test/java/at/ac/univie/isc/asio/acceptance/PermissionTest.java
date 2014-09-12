package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.jaxrs.Mime;
import at.ac.univie.isc.asio.tool.FunctionalTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
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
    assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
  }

  @Test
  public void should_deny_query_with_invalid_permission() throws Exception {
    response = client().path("none").path("sql").request()
        .post(Entity.entity("SELECT 1", Mime.QUERY_SQL.type()));
    assertThat(response.getStatusInfo().getFamily(), is(Family.CLIENT_ERROR));
  }

  @Test
  public void should_deny_update_with_read_permission() throws Exception {
    response = client().path("read").path("sql").request()
        .post(Entity.entity("CREATE TABLE nulltable", Mime.UPDATE_SQL.type()));
    assertThat(response.getStatusInfo().getFamily(), is(Family.CLIENT_ERROR));
    // FIXME : assert nulltable has not been created in database
  }
}
