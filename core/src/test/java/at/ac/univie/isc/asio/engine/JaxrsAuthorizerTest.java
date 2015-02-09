package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.SecurityContextHolder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;

import static org.mockito.Mockito.when;

public class JaxrsAuthorizerTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Invocation invocation = Mockito.mock(Invocation.class);
  private final SecurityContext security = Mockito.mock(SecurityContext.class);
  private JaxrsAuthorizer subject = JaxrsAuthorizer.create();

  @After
  public void clearContext() {
    SecurityContextHolder.clear();
  }

  @Test
  public void should_pass_if_authorized() throws Exception {
    when(invocation.requires()).thenReturn(Role.ADMIN);
    when(security.isUserInRole(Role.ADMIN.name())).thenReturn(true);
    SecurityContextHolder.set(security);
    subject.check(invocation);
  }

  @Test
  public void should_fail_if_not_authorized() throws Exception {
    when(invocation.requires()).thenReturn(Role.ADMIN);
    when(security.isUserInRole(Role.ADMIN.name())).thenReturn(false);
    SecurityContextHolder.set(security);
    error.expect(ForbiddenException.class);
    subject.check(invocation);
  }

  @Test
  public void should_fail_if_no_context_available() throws Exception {
    when(invocation.requires()).thenReturn(Role.ADMIN);
    error.expect(IllegalStateException.class);
    subject.check(invocation);
  }
}
