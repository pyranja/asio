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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;

import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class UriAuthenticationFilterTest {
  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final UriAuthenticationFilter subject = UriAuthenticationFilter.create();

  @Parameterized.Parameters(name = "{index}: should extract [{1}] from <{0}>")
  public static Iterable<Object[]> uriAndPrincipal() {
    // { request-uri, expected-principal }
    return Arrays.asList(new Object[][] {
        {"/head/principal", "principal"},
        {"/head/principal/", "principal"},
        {"/head/principal/tail", "principal"},
        {"/head/principal/tail/and/more", "principal"},
        {"/head/principal#fragment", "principal"},
        {"/head/principal#fragment/", "principal"},
        {"/head/principal;matrix=param", "principal"},
        {"/head/principal;matrix=param/", "principal"},
        // this is actually not allowed by servlet API
        {"/head/principal?query=param", "principal"},
        {"/head/principal?query=param/", "principal"},
        // do not get fooled by encoded slashes
        {"/head/principal%2Ftail", "principal"},
        {"/head%2Fprincipal/tail", "principal"},
        {"/head%2Fprincipal%2Ftail", "principal"},
        // illegal uris
        {"/head/", null},
        {"/head//test", null},
        {"/head/#fragment/test", null},
        {"/head/;matrix=param/test", null},
    });
  }

  @Parameterized.Parameter(0)
  public String uri;
  @Parameterized.Parameter(1)
  public Object principal;

  @Test
  public void should_find_principal_in_request_uri() throws Exception {
    request.setRequestURI(uri);
    assertThat(subject.getPreAuthenticatedPrincipal(request), Matchers.equalTo(principal));
  }

  @Test
  public void should_ignore_context_path() throws Exception {
    request.setContextPath("/context");
    request.setRequestURI("/context" + uri);
    assertThat(subject.getPreAuthenticatedPrincipal(request), Matchers.equalTo(principal));
  }

  @Test
  public void should_return__NA__as_credentials() throws Exception {
    request.setRequestURI(uri);
    assertThat(subject.getPreAuthenticatedCredentials(request), Matchers.<Object>equalTo("N/A"));
  }
}
