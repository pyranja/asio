package at.ac.univie.isc.asio.security;

import org.junit.Test;

import static com.google.common.io.BaseEncoding.base64;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BasicAuthIdentityExtractorTest {
  private final BasicAuthIdentityExtractor subject = new BasicAuthIdentityExtractor();

  @Test
  public void should_return_undefined_if_no_auth_given() throws Exception {
    final Identity identity = subject.authenticate(null);
    assertThat(identity, is(Identity.undefined()));
  }

  @Test
  public void should_return_undefined_on_empty_header_value() throws Exception {
    final Identity identity = subject.authenticate("");
    assertThat(identity, is(Identity.undefined()));
  }

  @Test(expected = BasicAuthIdentityExtractor.MalformedAuthHeader.class)
  public void should_fail_on_non_basic_auth() throws Exception {
    subject.authenticate("NONBASIC ABC");
  }

  @Test(expected = BasicAuthIdentityExtractor.MalformedAuthHeader.class)
  public void should_fail_when_no_encoded_credentials_given() throws Exception {
    subject.authenticate("Basic ");
  }

  @Test(expected = BasicAuthIdentityExtractor.MalformedAuthHeader.class)
  public void should_fail_on_malformed_credentials() throws Exception {
    final String malformed = base64().encode("no_colon".getBytes());
    subject.authenticate("Basic " + malformed);
  }

  @Test
  public void should_auth_with_empty_name_and_secret_if_missing_in_header() throws Exception {
    final String credentials = base64().encode(":".getBytes());
    final Identity identity = subject.authenticate("Basic " + credentials);
    assertThat(identity, is(Identity.from("", "")));
  }

  @Test
  public void should_auth_with_given_name_and_password() throws Exception {
    final String credentials = base64().encode("test-identity:test-password".getBytes());
    final Identity identity = subject.authenticate("Basic " + credentials);
    assertThat(identity, is(Identity.from("test-identity", "test-password")));
  }
}
