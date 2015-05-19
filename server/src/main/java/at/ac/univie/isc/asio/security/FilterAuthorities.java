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
