package at.ac.univie.isc.asio.security;

import com.google.common.collect.ImmutableSet;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Set;

/**
 * Exclude a configurable set of permissions if the request method is {@code GET}.
 */
public final class GetMethodRestriction {
  /**
   * Create a restriction that excludes the given authorities if the request method is GET.
   *
   * @param excluded authorities to be denied
   * @return restriction
   */
  @SafeVarargs
  public static <AUTHORITY extends GrantedAuthority> GetMethodRestriction exclude(final AUTHORITY... excluded) {
    return new GetMethodRestriction(ImmutableSet.copyOf(excluded));
  }

  private final Set<? extends GrantedAuthority> excluded;

  private GetMethodRestriction(final Set<? extends GrantedAuthority> excluded) {
    this.excluded = excluded;
  }

  public Collection<GrantedAuthority> filter(final Iterable<? extends GrantedAuthority> authorities,
                                             final HttpServletRequest request) {
    if (HttpMethod.GET.name().equals(request.getMethod())) {
      return omitExcludedFrom(authorities);
    } else {
      return ImmutableSet.copyOf(authorities);
    }
  }

  private Collection<GrantedAuthority> omitExcludedFrom(final Iterable<? extends GrantedAuthority> authorities) {
    final ImmutableSet.Builder<GrantedAuthority> filtered = ImmutableSet.builder();
    for (GrantedAuthority authority : authorities) {
      if (!excluded.contains(authority)) {
        filtered.add(authority);
      }
    }
    return filtered.build();
  }

  @Override
  public String toString() {
    return "GetMethodRestriction{excluded=" + excluded + '}';
  }
}
