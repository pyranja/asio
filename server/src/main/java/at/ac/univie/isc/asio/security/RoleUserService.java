/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
