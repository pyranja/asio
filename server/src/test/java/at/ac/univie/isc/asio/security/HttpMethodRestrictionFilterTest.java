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

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class HttpMethodRestrictionFilterTest {
  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final MockFilterChain chain = new MockFilterChain();

  private final HttpMethodRestrictionFilter subject = new HttpMethodRestrictionFilter();

  @After
  public void clearSecurityContext() {
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  @Test
  public void should_ignore_non__GET__request() throws Exception {
    final TestingAuthenticationToken token = new TestingAuthenticationToken("user", "secret");
    setAuthentication(token);
    request.setMethod(HttpMethod.POST.name());
    subject.doFilter(request, response, chain);
    assertThat(getAuthentication(), Matchers.<Authentication>sameInstance(token));
  }

  @Test
  public void should_ignore_request_without_authentication() throws Exception {
    request.setMethod(HttpMethod.GET.name());
    subject.doFilter(request, response, chain);
    assertThat(getAuthentication(), nullValue());
  }

  @Test
  public void should_remove__PERMISSION_UPDATE__from__GET__request() throws Exception {
    final TestingAuthenticationToken token =
        new TestingAuthenticationToken("user", "secret", Arrays.<GrantedAuthority>asList(Permission.INVOKE_QUERY, Permission.INVOKE_UPDATE));
    setAuthentication(token);
    request.setMethod(HttpMethod.GET.name());
    subject.doFilter(request, response, chain);
    assertThat(getAuthentication().getAuthorities(), Matchers.<GrantedAuthority>contains(Permission.INVOKE_QUERY));
  }

  @Test
  public void should_keep_other_token_properties() throws Exception {
    final TestingAuthenticationToken token =
        new TestingAuthenticationToken("user", "secret", Collections.<GrantedAuthority>singletonList(Permission.INVOKE_UPDATE));
    token.setDetails("details");
    setAuthentication(token);
    request.setMethod(HttpMethod.GET.name());
    subject.doFilter(request, response, chain);
    final Authentication filtered = getAuthentication();
    assertThat(filtered.getPrincipal(), equalTo(token.getPrincipal()));
    assertThat(filtered.getCredentials(), equalTo(token.getCredentials()));
    assertThat(filtered.getDetails(), equalTo(token.getDetails()));
  }

  @Test
  public void should_keep_anonymous_type() throws Exception {
    final AnonymousAuthenticationToken token =
        new AnonymousAuthenticationToken("key", "principal", Collections.<GrantedAuthority>singletonList(Permission.INVOKE_UPDATE));
    token.setDetails("details");
    setAuthentication(token);
    request.setMethod(HttpMethod.GET.name());
    subject.doFilter(request, response, chain);
    final Authentication filtered = getAuthentication();
    assertThat(filtered, instanceOf(AnonymousAuthenticationToken.class));
    assertThat(filtered.getPrincipal(), equalTo(token.getPrincipal()));
    assertThat(filtered.getDetails(), equalTo(token.getDetails()));
  }

  @Test
  public void should_keep_rememberme_type() throws Exception {
    final RememberMeAuthenticationToken token =
        new RememberMeAuthenticationToken("key", "principal", Collections.<GrantedAuthority>singletonList(Permission.INVOKE_UPDATE));
    token.setDetails("details");
    setAuthentication(token);
    request.setMethod(HttpMethod.GET.name());
    subject.doFilter(request, response, chain);
    final Authentication filtered = getAuthentication();
    assertThat(filtered, instanceOf(RememberMeAuthenticationToken.class));
    assertThat(filtered.getPrincipal(), equalTo(token.getPrincipal()));
    assertThat(filtered.getDetails(), equalTo(token.getDetails()));
  }

  private Authentication getAuthentication() {
    return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
  }

  private void setAuthentication(final Authentication auth) {
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
