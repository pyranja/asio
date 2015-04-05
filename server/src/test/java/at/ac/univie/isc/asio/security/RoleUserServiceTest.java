package at.ac.univie.isc.asio.security;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class RoleUserServiceTest {

  private final RoleUserService<Authentication> subject = RoleUserService.create();

  @DataPoints
  public static Role[] roles() {
    return Role.values();
  }

  @Theory
  public void should_yield_identical_users_from_both_service_contracts(final Role role) {
    final UserDetails by_username = subject.loadUserByUsername(role.name());
    final TestingAuthenticationToken token = new TestingAuthenticationToken(role.name(), "N/A");
    final UserDetails by_token = subject.loadUserDetails(token);
    assertThat(by_username, equalTo(by_token));
  }

  @Theory
  public void should_yield_set_username_to_role_name(final Role role) {
    final TestingAuthenticationToken token = new TestingAuthenticationToken(role.name(), "N/A");
    final UserDetails user = subject.loadUserDetails(token);
    assertThat(user.getUsername(), equalTo(role.name()));
  }

  @Theory
  public void should_ignore_casing_on_username(final Role role) throws Exception {
    final String lowerRoleName = role.name().toLowerCase(Locale.ENGLISH);
    final TestingAuthenticationToken token = new TestingAuthenticationToken(lowerRoleName, "N/A");
    final UserDetails user = subject.loadUserDetails(token);
    assertThat(user.getUsername(), equalTo(role.name()));
  }

  @Theory
  public void should_set_password_to__NA(final Role role) {
    final TestingAuthenticationToken token = new TestingAuthenticationToken(role.name(), "N/A");
    final UserDetails user = subject.loadUserDetails(token);
    assertThat(user.getPassword(), equalTo("N/A"));
  }

  @SuppressWarnings("unchecked")
  @Theory
  public void should_add_role_to_authorities(final Role role) {
    final TestingAuthenticationToken token = new TestingAuthenticationToken(role.name(), "N/A");
    final UserDetails user = subject.loadUserDetails(token);
    assertThat((Iterable<GrantedAuthority>) user.getAuthorities(), hasItem(equalTo(role)));
  }

  @SuppressWarnings("unchecked")
  @Theory
  public void should_add_mapped_authorities(final Role role) {
    final TestingAuthenticationToken token = new TestingAuthenticationToken(role.name(), "N/A");
    final UserDetails user = subject.loadUserDetails(token);
    final GrantedAuthority[] expected = role.getGrantedAuthorities().toArray(new GrantedAuthority[role.getGrantedAuthorities().size()]);
    assertThat((Iterable<GrantedAuthority>) user.getAuthorities(), hasItems(expected));
  }

  @Test(expected = UsernameNotFoundException.class)
  public void should_fail_if_given_principal_not_a_role_name() throws Exception {
    final TestingAuthenticationToken token = new TestingAuthenticationToken("test", "N/A");
    subject.loadUserDetails(token);
  }
}
