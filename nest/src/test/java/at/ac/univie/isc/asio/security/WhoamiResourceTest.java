package at.ac.univie.isc.asio.security;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WhoamiResourceTest {

  private final SecurityContext security = Mockito.mock(SecurityContext.class);
  private final WhoamiResource subject = new WhoamiResource(security);

  @Test
  public void should_provide_authentication_principal_name() throws Exception {
    when(security.getAuthentication())
        .thenReturn(new TestingAuthenticationToken("test-name", "password"));
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getLogin(), equalTo("test-name"));
  }

  @Test
  public void should_omit_delegated_credentials_if_not_instance_of_identity() throws Exception {
    final TestingAuthenticationToken auth = new TestingAuthenticationToken("name", "password");
    auth.setDetails("not delegated credentials");
    when(security.getAuthentication()).thenReturn(auth);
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getName(), nullValue());
    assertThat(response.getSecret(), nullValue());
  }

  @Test
  public void should_include_identity_if_present() throws Exception {
    final TestingAuthenticationToken auth = new TestingAuthenticationToken("name", "password");
    auth.setDetails(new DelegatedCredentialsDetails(Identity.from("test-login", "test-secret")));
    when(security.getAuthentication()).thenReturn(auth);
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getName(), equalTo("test-login"));
    assertThat(response.getSecret(), equalTo("test-secret"));
  }

  @Test
  public void should_include_granted_authorities() throws Exception {
    when(security.getAuthentication())
        .thenReturn(new TestingAuthenticationToken("name", "password", "one", "two"));
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getAuthorities(), containsInAnyOrder("one", "two"));
  }
}
