package at.ac.univie.isc.asio.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.*;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A user service that generates user details based on existing {@link Role roles}.
 * If an authentication token has a principal, which is equal to a role name the returned user
 * details will hold the authorities of that role.
 */
public final class RoleUserService<AUTH extends Authentication>
    implements AuthenticationUserDetailsService<AUTH>, UserDetailsService {

  /**
   * Create a new user service.
   */
  public static <AUTH extends Authentication> RoleUserService<AUTH> create() {
    return new RoleUserService<>();
  }

  /**
   * Convenience creator to avoid generics when only a {@code UserDetailsService} is required.
   */
  public static UserDetailsService asUserDetailsService() {
    return new RoleUserService<>();
  }

  private final Map<String, UserDetails> users;

  private RoleUserService() {
    final ImmutableMap.Builder<String, UserDetails> users = ImmutableMap.builder();
    for (Role role : Role.values()) {
      final Set<GrantedAuthority> authorities = ImmutableSet.<GrantedAuthority>builder()
          .add(role).addAll(role.getGrantedAuthorities()).build();
      users.put(role.name(), new User(role.name(), "N/A", authorities));
    }
    this.users = users.build();
  }

  @Override
  public UserDetails loadUserByUsername(final String principal) throws UsernameNotFoundException {
    final String roleName = principal.toUpperCase(Locale.ENGLISH);
    final UserDetails user = users.get(roleName);
    if (user == null) {
      throw new UsernameNotFoundException("unknown role <" + principal + ">");
    }
    return user;
  }

  @Override
  public UserDetails loadUserDetails(final AUTH token) throws UsernameNotFoundException {
    return loadUserByUsername(token.getName());
  }
}
