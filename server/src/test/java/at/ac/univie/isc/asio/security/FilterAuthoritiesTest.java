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

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FilterAuthoritiesTest {
  @Test
  public void should_yield_input_if_none_excluded() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(AuthorityUtils.NO_AUTHORITIES);
    final Set<GrantedAuthority> mapped = subject.mapAuthorities(authorities("one", "two"));
    assertThat(mapped, containsOnly("one", "two"));
  }

  @Test
  public void should_remove_excluded_authorities_from_input() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(authorities("exclude-me"));
    final Set<GrantedAuthority> mapped = subject.mapAuthorities(authorities("stay", "exclude-me"));
    assertThat(mapped, containsOnly("stay"));
  }

  @Test
  public void should_yield_input_if_no_excluded_present() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(authorities("exclude-me"));
    final Set<GrantedAuthority> mapped = subject.mapAuthorities(authorities("stay", "stay-too"));
    assertThat(mapped, containsOnly("stay", "stay-too"));
  }

  @Test
  public void should_always_create_a_copy() throws Exception {
    final FilterAuthorities subject = FilterAuthorities.exclude(AuthorityUtils.NO_AUTHORITIES);
    final Collection<GrantedAuthority> input = authorities("one", "two");
    final Collection<GrantedAuthority> output = subject.mapAuthorities(input);
    assertThat(output, not(sameInstance(input)));
  }

  private List<GrantedAuthority> authorities(final String... authorityNames) {
    return AuthorityUtils.createAuthorityList(authorityNames);
  }

  private Matcher<Iterable<?>> containsOnly(final String... expectedNames) {
    return containsInAnyOrder(authorities(expectedNames).toArray());
  }
}
