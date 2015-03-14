package at.ac.univie.isc.asio.security;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FixedAuthorityFinderTest {

  @Test
  public void should_not_redirect() throws Exception {
    final FindAuthorization.AuthAndRedirect result =
        FixedAuthorityFinder.create(new SimpleGrantedAuthority("test")).accept(new MockHttpServletRequest());
    assertThat(result.redirection().isPresent(), is(false));
  }

  @Test
  public void should_return_fixed_authority() throws Exception {
    final FindAuthorization.AuthAndRedirect result =
        FixedAuthorityFinder.create(new SimpleGrantedAuthority("test")).accept(new MockHttpServletRequest());
    assertThat(result.authority(), Matchers.<GrantedAuthority>is(new SimpleGrantedAuthority("test")));
  }
}
