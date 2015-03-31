package at.ac.univie.isc.asio.security;

import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AuthToolsTest {

  private final SecurityContextImpl context = new SecurityContextImpl();

  @Test
  public void should_find_identity_in_authentication_details() throws Exception {
    final TestingAuthenticationToken authentication = new TestingAuthenticationToken("test", "password");
    authentication.setDetails(new DelegatedCredentialsDetails(Identity.from("user", "password")));
    context.setAuthentication(authentication);
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.from("user", "password")));
  }

  @Test
  public void should_yield_undefined_if_context_empty() throws Exception {
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.undefined()));
  }

  @Test
  public void should_yield_undefined_if_authentication_has_no_details() throws Exception {
    context.setAuthentication(new TestingAuthenticationToken("user", "password"));
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.undefined()));
  }

  @Test
  public void should_yield_undefined_if_unexpected_details_type_found() throws Exception {
    final TestingAuthenticationToken authentication = new TestingAuthenticationToken("test", "password");
    authentication.setDetails("no delegated credentials");
    context.setAuthentication(authentication);
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.undefined()));
  }
}
