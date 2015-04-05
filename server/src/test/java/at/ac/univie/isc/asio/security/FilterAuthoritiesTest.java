package at.ac.univie.isc.asio.security;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FilterAuthoritiesTest {
  @Test
  public void should_yield_input_if_none_excluded() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(AuthorityUtils.NO_AUTHORITIES);
    final Set<GrantedAuthority> mapped = subject.mapAuthorities(authorities("one", "two"));
    assertThat(mapped, containsOnly("one", "two"));
  }

  @Test
  public void should_remove_excluded_authorities_from_input() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(authorities("exclude-me"));
    final Set<GrantedAuthority> mapped = subject.mapAuthorities(authorities("stay", "exclude-me"));
    assertThat(mapped, containsOnly("stay"));
  }

  @Test
  public void should_yield_input_if_no_excluded_present() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(authorities("exclude-me"));
    final Set<GrantedAuthority> mapped = subject.mapAuthorities(authorities("stay", "stay-too"));
    assertThat(mapped, containsOnly("stay", "stay-too"));
  }

  @Test
  public void should_always_create_a_copy() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(AuthorityUtils.NO_AUTHORITIES);
    final Collection<GrantedAuthority> input = authorities("one", "two");
    final Collection<GrantedAuthority> output = subject.mapAuthorities(input);
    assertThat(output, not(sameInstance(input)));
  }

  private List<GrantedAuthority> authorities(final String... authorityNames) {
    return AuthorityUtils.createAuthorityList(authorityNames);
  }

  private Matcher<Iterable<?>> containsOnly(final String... expectedNames) {
    return containsInAnyOrder(authorities(expectedNames).toArray());
  }
}
