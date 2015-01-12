package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class CheckCommandAuthorizationTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Command command = Mockito.mock(Command.class);
  private final SecurityContext security = Mockito.mock(SecurityContext.class);
  private final Request request = Mockito.mock(Request.class);
  private final CheckCommandAuthorization subject =
      CheckCommandAuthorization.with(security, request);

  @Before
  public void setUp() {
    when(command.requiredRole()).thenReturn(Role.READ);
    when(request.getMethod()).thenReturn(HttpMethod.GET);
  }

  @Test
  public void allow_if_role_matches() throws Exception {
    when(security.isUserInRole(anyString())).thenReturn(true);
    assertThat(subject.check(command), is(true));
  }

  @Test
  public void forbid_if_client_is_not_in_required_role() throws Exception {
    when(security.isUserInRole(anyString())).thenReturn(false);
    error.expect(ForbiddenException.class);
    subject.check(command);
  }

  @Test
  public void forbid_write_if_request_is_GET() throws Exception {
    when(security.isUserInRole(anyString())).thenReturn(true);
    when(command.requiredRole()).thenReturn(Role.WRITE);
    error.expect(ForbiddenException.class);
    subject.check(command);
  }
}
