package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.SecurityContextHolder;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ValidatingInvokerTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Invoker delegate = Mockito.mock(Invoker.class);
  private final SecurityContext security = Mockito.mock(SecurityContext.class);
  private ValidatingInvoker subject = ValidatingInvoker.around(delegate);

  @Before
  public void injectContext() {
    when(security.isUserInRole(anyString())).thenReturn(true);
    SecurityContextHolder.set(security);
  }

  @After
  public void clearContext() {
    SecurityContextHolder.clear();
  }

  @Test
  public void should_forward_invocation_from_delegate() throws Exception {
    final Invocation expected = Mockito.mock(Invocation.class);
    final Parameters parameters = ParametersBuilder.dummy();
    when(expected.requires()).thenReturn(Role.READ);
    when(delegate.prepare(parameters)).thenReturn(expected);
    final Invocation actual = subject.prepare(parameters);
    assertThat(actual, is(expected));
  }

  @Test
  public void should_escalate_failure_from_parameters() throws Exception {
    error.expect(IllegalArgumentException.class);
    subject.prepare(ParametersBuilder.invalid(new IllegalArgumentException()));
  }

  @Test
  public void should_forbid_execution_if_client_not_authorized() throws Exception {
    error.expect(ForbiddenException.class);
    final Invocation expected = Mockito.mock(Invocation.class);
    when(expected.requires()).thenReturn(Role.ADMIN);
    when(delegate.prepare(Mockito.any(Parameters.class))).thenReturn(expected);
    when(security.isUserInRole(Role.ADMIN.name())).thenReturn(false);
    subject.prepare(ParametersBuilder.dummy());
  }
}
