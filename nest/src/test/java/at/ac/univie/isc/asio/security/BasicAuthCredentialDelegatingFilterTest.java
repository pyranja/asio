package at.ac.univie.isc.asio.security;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class BasicAuthCredentialDelegatingFilterTest {

  private final BasicAuthCredentialDelegatingFilter subject =
      new BasicAuthCredentialDelegatingFilter(new BasicAuthIdentityExtractor());
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  public void should_retrieve_basic_auth_username_as_principal() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, credentials("principal", "password"));
    final Object result = subject.getPreAuthenticatedPrincipal(request);
    assertThat(result, Matchers.<Object>is("principal"));
  }

  @Test
  public void should_retrieve_full_basic_auth_token_as_credentials() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, credentials("principal", "password"));
    final Object result = subject.getPreAuthenticatedCredentials(request);
    assertThat(result, Matchers.<Object>is(Identity.from("principal", "password")));
  }

  @Test
  public void should_return_empty_as_principal_if_empty_username() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, credentials("", "password"));
    final Object result = subject.getPreAuthenticatedPrincipal(request);
    assertThat(result, Matchers.<Object>is(""));
  }

  @Test
  public void should_return_anonymous_token_as_credentials_if_empty_username() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, credentials("", "password"));
    final Object result = subject.getPreAuthenticatedCredentials(request);
    assertThat(result, Matchers.<Object>is(Identity.from("", "password")));
  }

  @Test
  public void should_return_null_principal_if_header_is_missing() throws Exception {
    final Object result = subject.getPreAuthenticatedPrincipal(request);
    assertThat(result, is(nullValue()));
  }

  @Test(expected = BadCredentialsException.class)
  public void should_wrap_extractor_error_in_spring_exception() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, "Digest 12345");
    subject.getPreAuthenticatedPrincipal(request);
  }

  private String credentials(final String principal, final String password) {
    final String userAndPassword = principal + ":" + password;
    final String credentials = BaseEncoding.base64().encode(userAndPassword.getBytes(Charsets.UTF_8));
    return "Basic " + credentials;
  }
}
