package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.BaseEncoding;
import org.apache.http.HttpHeaders;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.Enumeration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TranslateToDelegateAuthorizationTest {

  public static final String SHARED_SECRET = "secret";

  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final TranslateToDelegateAuthorization subject =
      TranslateToDelegateAuthorization.withSecret(SHARED_SECRET);
  private TranslateAuthorization.Wrapped translated;

  @Test
  public void override_basic_auth() throws Exception {
    translated = subject.translate(new SimpleGrantedAuthority("admin"), request, response);
    assertThat(basicAuth(translated.request()), is(Identity.from("admin", SHARED_SECRET)));
  }

  @Test
  public void hide_source_basic_auth() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, encode(Identity.from("test-name", "test-secret")));
    translated = subject.translate(new SimpleGrantedAuthority("admin"), request, response);
    final Enumeration<String> basicAuthHeaders =
        translated.request().getHeaders(HttpHeaders.AUTHORIZATION);
    assertThat(Collections.list(basicAuthHeaders), hasSize(1));
  }

  @Test
  public void copy_source_basic_auth_as_delegated() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, encode(Identity.from("test-name", "test-secret")));
    translated = subject.translate(new SimpleGrantedAuthority("admin"), request, response);
    assertThat(delegateAuth(translated.request()), is(Identity.from("test-name", "test-secret")));
  }

  @Test
  public void hide_source_delegated_auth() throws Exception {
    request.addHeader("Delegate-Authorization", encode(Identity.from("test-name", "test-secret")));
    translated = subject.translate(new SimpleGrantedAuthority("admin"), request, response);
    final Enumeration<String> delegateAuthHeaders =
        translated.request().getHeaders("Delegate-Authorization");
    assertThat(Collections.list(delegateAuthHeaders), hasSize(1));
  }

  private String encode(final Identity identity) {
    return "Basic " + BaseEncoding.base64().encode(
        Payload.encodeUtf8(identity.getName() + ":" + identity.getSecret())
    );
  }

  private Identity basicAuth(final HttpServletRequest request) {
    return new BasicAuthIdentityExtractor().convert(request.getHeader(HttpHeaders.AUTHORIZATION));
  }

  private Identity delegateAuth(final HttpServletRequest request) {
    return new BasicAuthIdentityExtractor().convert(request.getHeader("Delegate-Authorization"));
  }
}
