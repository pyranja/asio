package at.aca.univie.isc.asio.security;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.security.Principal;

import javax.ws.rs.core.HttpHeaders;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.security.Anonymous;
import at.ac.univie.isc.asio.security.VphToken;
import at.ac.univie.isc.asio.security.VphTokenExtractor;

import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;

@RunWith(MockitoJUnitRunner.class)
public class VphTokenExtractorTest {

  private final VphTokenExtractor subject = new VphTokenExtractor();
  @Mock
  private HttpHeaders headers;

  @Test
  public void should_return_anonymous_if_no_auth_given() throws Exception {
    setAuthHeader();
    final Principal user = subject.authenticate(headers);
    assertThat(user, is(Anonymous.INSTANCE));
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
    final String malformed = BaseEncoding.base64().encode("no_colon".getBytes());
    setAuthHeader("Basic " + malformed);
    subject.authenticate(headers);
  }

  @Test
  public void should_auth_with_empty_token_if_password_is_empty() throws Exception {
    final String credentials = BaseEncoding.base64().encode(":".getBytes());
    setAuthHeader("Basic " + credentials);
    final VphToken user = (VphToken) subject.authenticate(headers);
    assertThat(user.getToken(), is(new char[] {}));
  }

  @Test
  public void should_auth_with_given_password_as_token() throws Exception {
    final String credentials = BaseEncoding.base64().encode(":test-password".getBytes());
    setAuthHeader("Basic " + credentials);
    final VphToken user = (VphToken) subject.authenticate(headers);
    assertThat(user.getToken(), is(new char[] {'t', 'e', 's', 't', '-', 'p', 'a', 's', 's', 'w',
        'o', 'r', 'd'}));
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_if_username_and_password_given() throws Exception {
    final String credentials = BaseEncoding.base64().encode("test-user:test-password".getBytes());
    setAuthHeader("Basic " + credentials);
    subject.authenticate(headers);
  }

  private void setAuthHeader(final String... values) {
    when(headers.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(
        ImmutableList.copyOf(values));
  }
}
