package at.ac.univie.isc.asio.security;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import java.security.Principal;

import static org.junit.Assert.assertThat;

public class DelegationDetailsSourceTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final DelegationDetailsSource subject =
      DelegationDetailsSource.usingHeader("Delegate-Authorization");
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  public void should_set_undefined_credentials_if_header_missing() throws Exception {
    final DelegatedCredentialsDetails details = subject.buildDetails(request);
    assertThat(details.getCredentials(), eq(Identity.undefined()));
  }

  @Test
  public void should_inject_credentials_contained_in_auth_header() throws Exception {
    final String header = "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8));
    request.addHeader("Delegate-Authorization", header);
    final DelegatedCredentialsDetails details = subject.buildDetails(request);
    assertThat(details.getCredentials(), eq(Identity.from("user", "password")));
  }

  @Test
  public void should_fail_fast_on_malformed_auth_header() throws Exception {
    request.addHeader("Delegate-Authorization", "IllegalScheme ABCDE");
    error.expect(BadCredentialsException.class);
    subject.buildDetails(request);
  }

  private Matcher<Principal> eq(final Identity expected) {
    return Matchers.<Principal>is(expected);
  }
}
