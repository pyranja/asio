package at.ac.univie.isc.asio.security;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthoritiesContainer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.Collection;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Map authorities by expanding all {@link org.springframework.security.core.authority.GrantedAuthoritiesContainer}
 * to their contained authorities.
 */
public final class ExpandAuthoritiesContainer implements GrantedAuthoritiesMapper {
  private static final Logger log = getLogger(ExpandAuthoritiesContainer.class);

  private ExpandAuthoritiesContainer() {}

  public static ExpandAuthoritiesContainer instance() {
    return new ExpandAuthoritiesContainer();
  }

  /**
   * Map all {@link org.springframework.security.core.authority.GrantedAuthoritiesContainer authority container}
   * to the contained authorities and drop all others.
   * Only first level members are expanded, i.e. nested containers are not supported.
   *
   * @param authorities source authorities
   * @return authorities contained in given ones
   */
  @Override
  public Set<GrantedAuthority> mapAuthorities(final Collection<? extends GrantedAuthority> authorities) {
    final ImmutableSet.Builder<GrantedAuthority> mapped = ImmutableSet.builder();
    for (final GrantedAuthoritiesContainer container : Iterables.filter(authorities, GrantedAuthoritiesContainer.class)) {
      for (final GrantedAuthority each : container.getGrantedAuthorities()) {
        mapped.add(each);
      }
    }
    final ImmutableSet<GrantedAuthority> result = mapped.build();
    log.debug("mapped source authority containers {} to contained authorities {}", authorities, result);
    return result;
  }
}
