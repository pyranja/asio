package at.ac.univie.isc.asio.security;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthoritiesContainer;
import org.springframework.security.core.authority.mapping.Attributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.Collection;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Map asio {@link Role roles} to {@link Permission permissions}.
 */
public final class RoleToPermissionMapper
    implements GrantedAuthoritiesMapper, Attributes2GrantedAuthoritiesMapper {
  private static final Logger log = getLogger(RoleToPermissionMapper.class);

  private RoleToPermissionMapper() {}

  public static RoleToPermissionMapper instance() {
    return new RoleToPermissionMapper();
  }

  @Override
  public Set<Permission> getGrantedAuthorities(final Collection<String> attributes) {
    final ImmutableSet.Builder<Permission> authorities = ImmutableSet.builder();
    for (final String attribute : attributes) {
      final Role role = Role.fromString(attribute);
      authorities.addAll(role.getGrantedAuthorities());
    }
    log.debug("mapped attributes {} to permissions {}", attributes, authorities);
    return authorities.build();
  }

  /**
   * Map all {@link org.springframework.security.core.authority.GrantedAuthoritiesContainer authority container}
   * to the contained authorities and drop all others.
   *
   * @param authorities source authorities
   * @return authorities contained in given ones
   */
  @Override
  public Set<GrantedAuthority> mapAuthorities(final Collection<? extends GrantedAuthority> authorities) {
    final ImmutableSet.Builder<GrantedAuthority> mapped = ImmutableSet.builder();
    for (final GrantedAuthoritiesContainer container : Iterables.filter(authorities, GrantedAuthoritiesContainer.class)) {
      for (GrantedAuthority each : container.getGrantedAuthorities()) {
        mapped.add(each);
      }
    }
    log.debug("mapped source authority containers {} to mapped authorities {}", authorities, mapped);
    return mapped.build();
  }
}
