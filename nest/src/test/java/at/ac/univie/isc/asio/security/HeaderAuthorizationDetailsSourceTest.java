package at.ac.univie.isc.asio.security;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.SimpleAttributes2GrantedAuthoritiesMapper;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;

import static at.ac.univie.isc.asio.security.HeaderAuthorizationDetailsSourceTest.AuthorityMatcher.authority;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class HeaderAuthorizationDetailsSourceTest {

  private final SimpleAttributes2GrantedAuthoritiesMapper mapper =
      new SimpleAttributes2GrantedAuthoritiesMapper();

  private HeaderAuthorizationDetailsSource subject =
      new HeaderAuthorizationDetailsSource("permission-header", mapper, GetMethodRestriction.exclude());

  private PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails result;
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Before
  public void configureMapper() {
    mapper.setAttributePrefix("");  // converts to authority with same text
  }

  @Test
  public void should_inject_no_authorities_if_header_missing() throws Exception {
    result = subject.buildDetails(new MockHttpServletRequest());
    assertThat(result.getGrantedAuthorities(), is(empty()));
    assertThat(result.getGrantedAuthorities(), is(AuthorityUtils.NO_AUTHORITIES));
  }

  @Test
  public void should_map_and_inject_a_single_authority() throws Exception {
    request.addHeader("permission-header", "authority");
    result = subject.buildDetails(request);
    assertThat(result.getGrantedAuthorities(), contains(authority("authority")));
  }

  @Test
  public void should_map_and_inject_multiple_comma_separated_authorities() throws Exception {
    request.addHeader("permission-header", "one,two");
    result = subject.buildDetails(request);
    assertThat(result.getGrantedAuthorities(), containsInAnyOrder(authority("one"), authority("two")));
  }

  @Test
  public void should_exclude_configured_authorities_on_a_GET_request() throws Exception {
    final GetMethodRestriction exclusion =
        GetMethodRestriction.exclude(new SimpleGrantedAuthority("exclude-me"));
    subject = new HeaderAuthorizationDetailsSource("permission-header", mapper, exclusion);
    request.addHeader("permission-header", "exclude-me,allow-me");
    request.setMethod(HttpMethod.GET.name());
    result = subject.buildDetails(request);
    assertThat(result.getGrantedAuthorities(), contains(authority("allow-me")));
  }

  @Test
  public void should_not_exclude_on_a_non_GET_request() throws Exception {
    final GetMethodRestriction exclusion =
        GetMethodRestriction.exclude(new SimpleGrantedAuthority("exclude-me"));
    subject = new HeaderAuthorizationDetailsSource("permission-header", mapper, exclusion);
    request.addHeader("permission-header", "exclude-me,allow-me");
    request.setMethod(HttpMethod.POST.name());
    result = subject.buildDetails(request);
    assertThat(result.getGrantedAuthorities(), containsInAnyOrder(authority("allow-me"), authority("exclude-me")));
  }

  static class AuthorityMatcher extends TypeSafeMatcher<GrantedAuthority> {
    public static AuthorityMatcher authority(final String expected) {
      return new AuthorityMatcher(new SimpleGrantedAuthority(expected));
    }

    private final GrantedAuthority expected;

    private AuthorityMatcher(final GrantedAuthority expected) {
      this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(final GrantedAuthority item) {
      return item.equals(expected);
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(expected);
    }
  }
}
