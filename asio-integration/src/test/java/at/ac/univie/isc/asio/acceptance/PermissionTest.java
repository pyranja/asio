package at.ac.univie.isc.asio.acceptance;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import at.ac.univie.isc.asio.tool.FunctionalTest;

@Category(FunctionalTest.class)
public class PermissionTest extends AcceptanceHarness {

  @Override
  protected URI getTargetUrl() {
    return AcceptanceHarness.SERVER_ADDRESS;
  }

  @Test
  public void should_fail_on_invalid_permission() throws Exception {
    client.path("invalid").accept(XML).query(PARAM_QUERY, "SELECT * FROM person");
    response = client.get();
    assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
  }

  @Test
  public void should_deny_query_with_invalid_permission() throws Exception {
    client.path("none").path("sql").accept(XML).query(PARAM_QUERY, "SELECT * FROM person");
    response = client.get();
    assertThat(response.getStatusInfo().getFamily(), is(Family.CLIENT_ERROR));
  }

  @Test
  public void should_deny_update_with_read_permission() throws Exception {
    client.path("read").path("sql").accept(XML)
        .query(PARAM_UPDATE, "INSERT INTO PATIENT VALUES (1337, 'test-name')");
    response = client.get();
    assertThat(response.getStatusInfo().getFamily(), is(Family.CLIENT_ERROR));
  }
}
