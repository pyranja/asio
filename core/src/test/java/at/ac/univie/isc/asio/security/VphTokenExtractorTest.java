package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.DatasetUsageException;
import com.google.common.base.Optional;
import org.junit.Test;

import java.security.Principal;

import static com.google.common.io.BaseEncoding.base64;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VphTokenExtractorTest {
  private final VphTokenExtractor subject = new VphTokenExtractor();

  @Test
  public void should_return_anonymous_if_no_auth_given() throws Exception {
    final Token user = subject.authenticate(Optional.<String>absent());
    assertThat(user, is(Token.ANONYMOUS));
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_on_empty_header_value() throws Exception {
    subject.authenticate(Optional.of(""));
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_on_non_basic_auth() throws Exception {
    subject.authenticate(Optional.of("NONBASIC ABC"));
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_when_no_encoded_credentials_given() throws Exception {
    subject.authenticate(Optional.of("Basic "));
  }

  @Test(expected = DatasetUsageException.class)
  public void should_fail_on_malformed_credentials() throws Exception {
    final String malformed = base64().encode("no_colon".getBytes());
    subject.authenticate(Optional.of("Basic "+ malformed));
  }

  @Test
  public void should_auth_with_empty_token_if_password_is_empty() throws Exception {
    final String credentials = base64().encode(":".getBytes());
    final Optional<String> header = Optional.of("Basic " + credentials);
    final Token user = subject.authenticate(header);
    assertThat(user.getToken(), is(""));
  }

  @Test
  public void should_auth_with_given_password_as_token() throws Exception {
    final String credentials = base64().encode(":test-password".getBytes());
    final Optional<String> header = Optional.of("Basic " + credentials);
    final Token user = subject.authenticate(header);
    assertThat(user.getToken(), is("test-password"));
  }

  @Test
  public void should_accept_username_and_password() throws Exception {
    final String credentials = base64().encode("test-user:test-password".getBytes());
    final Optional<String> header = Optional.of("Basic " + credentials);
    final Token principal = subject.authenticate(header);
    assertThat(principal.getName(), is("test-user"));
    assertThat(principal.getToken(), is("test-password"));
  }

  @Test
  public void should_set_username_on_token_if_given() throws Exception {
    final String credentials = base64().encode("test-user:".getBytes());
    final Optional<String> header = Optional.of("Basic " + credentials);
    final Principal principal = subject.authenticate(header);
    assertThat(principal.getName(), is("test-user"));
  }

  @Test
  public void should_set_empty_name_if_username_not_given() throws Exception {
    final String credentials = base64().encode(":test-password".getBytes());
    final Optional<String> header = Optional.of("Basic "+ credentials);
    final Principal principal = subject.authenticate(header);
    assertThat(principal.getName(), is(""));
  }
}
