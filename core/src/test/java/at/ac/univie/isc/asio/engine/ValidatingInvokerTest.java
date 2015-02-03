package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Role;
import com.google.common.base.Predicates;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import javax.ws.rs.ForbiddenException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ValidatingInvokerTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final Invoker delegate = Mockito.mock(Invoker.class);
  private ValidatingInvoker subject =
      ValidatingInvoker.around(Predicates.<Role>alwaysTrue(), delegate);

  @Test
  public void should_forward_invocation_from_delegate() throws Exception {
    final Invocation expected = Mockito.mock(Invocation.class);
    final Parameters parameters = ParametersBuilder.dummy();
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
    subject = ValidatingInvoker.around(Predicates.<Role>alwaysFalse(), delegate);
    error.expect(ForbiddenException.class);
    final Invocation expected = Mockito.mock(Invocation.class);
    when(expected.requires()).thenReturn(Role.ADMIN);
    when(delegate.prepare(Mockito.any(Parameters.class))).thenReturn(expected);
    subject.prepare(ParametersBuilder.dummy());
  }
}
