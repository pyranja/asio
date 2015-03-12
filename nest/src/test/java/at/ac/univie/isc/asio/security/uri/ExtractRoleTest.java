package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ExtractRoleTest {
  @Test
  public void should_handle_any() throws Exception {
    assertThat(ExtractRole.instance().canHandle(null), is(true));
  }

  @Test
  public void should_extract_role_from_path() throws Exception {
    final FindAuthorization.AuthAndRedirect result = ExtractRole.instance().handle(path("", "", "role"));
    assertThat(result.authority(), Matchers.<GrantedAuthority>is(new SimpleGrantedAuthority("role")));
  }

  @Test
  public void should_redirect_to_uri_without_authority() throws Exception {
    final FindAuthorization.AuthAndRedirect result = ExtractRole.instance().handle(path("head", "tail", "role"));
    assertThat(result.redirection().get(), is("headtail"));
  }

  private UriAuthRule.PathElements path(final String head, final String tail, final String role) {
    return UriAuthRule.PathElements.Mock.from(ImmutableMap.of("head", head, "tail", tail, "authority", role));
  }
}
