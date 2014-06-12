package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.DatasetUsageException;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.security.Principal;

import static com.google.common.io.BaseEncoding.base64;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class VphTokenExtractorTest {

  private final VphTokenExtractor subject = new VphTokenExtractor();
  @Mock
  private MultivaluedMap<String, String> headers;

  @Test
  public void should_return_anonymous_if_no_auth_given() throws Exception {
    setAuthHeader();
    final Token user = subject.authenticate(headers);
    assertThat(user, is(Token.ANONYMOUS));
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_if_multiple_headers_given() throws Exception {
    setAuthHeader("test", "test");
    subject.authenticate(headers);
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_on_empty_header_value() throws Exception {
    setAuthHeader("");
    subject.authenticate(headers);
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_on_non_basic_auth() throws Exception {
    setAuthHeader("NONBASIC ABC");
    subject.authenticate(headers);
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_when_no_encoded_credentials_given() throws Exception {
    setAuthHeader("Basic ");
    subject.authenticate(headers);
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_on_malformed_credentials() throws Exception {
    final String malformed = base64().encode("no_colon".getBytes());
    setAuthHeader("Basic " + malformed);
    subject.authenticate(headers);
  }

  @Test
  public void should_auth_with_empty_token_if_password_is_empty() throws Exception {
    final String credentials = base64().encode(":".getBytes());
    setAuthHeader("Basic " + credentials);
    final Token user = (Token) subject.authenticate(headers);
    assertThat(user.getToken(), is(""));
  }

  @Test
  public void should_auth_with_given_password_as_token() throws Exception {
    final String credentials = base64().encode(":test-password".getBytes());
    setAuthHeader("Basic " + credentials);
    final Token user = (Token) subject.authenticate(headers);
    assertThat(user.getToken(), is("test-password"));
  }

  @Test
  public void should_accept_username_and_password() throws Exception {
    final String credentials = base64().encode("test-user:test-password".getBytes());
    setAuthHeader("Basic " + credentials);
    final Token principal = (Token) subject.authenticate(headers);
    assertThat(principal.getName(), is("test-user"));
    assertThat(principal.getToken(), is("test-password"));
  }

  @Test
  public void should_set_username_on_token_if_given() throws Exception {
    final String credentials = base64().encode("test-user:".getBytes());
    setAuthHeader("Basic " + credentials);
    final Principal principal = subject.authenticate(headers);
    assertThat(principal.getName(), is("test-user"));
  }

  @Test
  public void should_set_dummy_if_username_not_given() throws Exception {
    final String credentials = base64().encode(":test-password".getBytes());
    setAuthHeader("Basic "+ credentials);
    final Principal principal = subject.authenticate(headers);
    assertThat(principal.getName(), is(Token.UNKNOWN_PRINCIPAL));
  }

  private void setAuthHeader(final String... values) {
    Mockito.when(headers.get(HttpHeaders.AUTHORIZATION)).thenReturn(
        ImmutableList.copyOf(values));
  }
}
