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
   * to themselves and their contained authorities.
   * Only first level members are expanded, i.e. nested containers are not supported.
   *
   * @param authorities source authorities
   * @return authorities plus all contained ones
   */
  @Override
  public Set<GrantedAuthority> mapAuthorities(final Collection<? extends GrantedAuthority> authorities) {
    final ImmutableSet.Builder<GrantedAuthority> mapped = ImmutableSet.builder();
    mapped.addAll(authorities);
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
