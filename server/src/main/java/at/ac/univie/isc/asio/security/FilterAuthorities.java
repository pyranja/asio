package at.ac.univie.isc.asio.security;

import com.google.common.collect.ImmutableSet;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.Collection;
import java.util.Set;

/**
 * A {@code GrantedAuthoritiesMapper} that will remove all excluded authorities from the given
 * set of authorities.
 */
public final class FilterAuthorities implements GrantedAuthoritiesMapper {
  private final Set<GrantedAuthority> excluded;

  private FilterAuthorities(final Iterable<GrantedAuthority> excluded) {
    this.excluded = ImmutableSet.copyOf(excluded);
  }

  public static FilterAuthorities exclude(final Iterable<GrantedAuthority> excluded) {
    return new FilterAuthorities(excluded);
  }

  /**
   * Remove all authorities, that have been excluded. Note: <strong>Always</strong> returns an
   * immutable copy of the input with {@link java.util.Set} semantics.
   *
   * @param authorities original set of authorities
   * @return all elements of authorities, that are not excluded
   */
  @Override
  public Set<GrantedAuthority> mapAuthorities(final Collection<? extends GrantedAuthority> authorities) {
    final ImmutableSet.Builder<GrantedAuthority> filtered = ImmutableSet.builder();
    for (final GrantedAuthority each : authorities) {
      if (!excluded.contains(each)) {
        filtered.add(each);
      }
    }
    return filtered.build();
  }

  @Override
  public String toString() {
    return "AuthoritiesFilter{" +
        "excluded=" + excluded +
        '}';
  }
}
