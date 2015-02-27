package at.ac.univie.isc.asio.security;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DelegationAwareAuthenticationProviderTest {

  private final DelegationAwareAuthenticationProvider subject = new DelegationAwareAuthenticationProvider();

  @Test
  public void should_not_interfere_if_no_delegated_credentials_details_given() throws Exception {
    final List<GrantedAuthority> expectedAuthorities = AuthorityUtils.createAuthorityList("one", "two");
    final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("name", "password");
    final User user = new User("name", "password", expectedAuthorities);
    final Authentication authentication = subject.createSuccessAuthentication(user, token, user);
    assertThat(authentication.isAuthenticated(), is(true));
    assertThat(authentication.getPrincipal(), Matchers.<Object>is(user));
    assertThat(authentication.getCredentials(), is(token.getCredentials()));
    assertThat(authentication.getAuthorities(), containsInAnyOrder(expectedAuthorities.toArray()));
  }

  @Test
  public void should_use_delegated_credentials_in_success_authentication() throws Exception {
    final List<GrantedAuthority> expectedAuthorities = AuthorityUtils.createAuthorityList("one", "two");
    final User user = new User("name", "password", expectedAuthorities);
    final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("name", "password");
    token.setDetails(new DelegatedCredentialsDetails(Identity.undefined()));
    final Authentication authentication = subject.createSuccessAuthentication(user, token, user);
    assertThat(authentication.getCredentials(), Matchers.<Object>is(Identity.undefined()));
  }

  @Test
  public void should_apply_restriction() throws Exception {
    final List<GrantedAuthority> originalAuthorities = AuthorityUtils.createAuthorityList("one", "exclude-me", "two");
    final User user = new User("name", "password", originalAuthorities);
    final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("name", "password");
    final DelegatedCredentialsDetails details = new DelegatedCredentialsDetails(Identity.undefined());
    details.setRestriction(AuthoritiesFilter.exclude(AuthorityUtils.createAuthorityList("exclude-me")));
    token.setDetails(details);
    final Authentication authentication = subject.createSuccessAuthentication(user, token, user);
    final List<GrantedAuthority> expectedAuthorities = AuthorityUtils.createAuthorityList("one", "two");
    assertThat(authentication.getAuthorities(), containsInAnyOrder(expectedAuthorities.toArray()));
  }

  @Test
  public void should_obey_parent_contract_if_injecting_delegating_credentials() throws Exception {
    final List<GrantedAuthority> expectedAuthorities = AuthorityUtils.createAuthorityList("one", "two");
    final User user = new User("name", "password", expectedAuthorities);
    final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("name", "password");
    token.setDetails(new DelegatedCredentialsDetails(Identity.undefined()));
    final Authentication authentication = subject.createSuccessAuthentication(user, token, user);
    assertThat(authentication.isAuthenticated(), is(true));
    assertThat(authentication.getPrincipal(), Matchers.<Object>is(user));
    assertThat(authentication.getAuthorities(), containsInAnyOrder(expectedAuthorities.toArray()));
  }
}
