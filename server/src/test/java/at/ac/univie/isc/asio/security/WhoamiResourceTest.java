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

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WhoamiResourceTest {

  private final SecurityContext security = Mockito.mock(SecurityContext.class);
  private final WhoamiResource subject = new WhoamiResource(security);

  @Test
  public void should_provide_authentication_principal_name() throws Exception {
    when(security.getAuthentication())
        .thenReturn(new TestingAuthenticationToken("test-name", "password"));
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getLogin(), equalTo("test-name"));
  }

  @Test
  public void should_omit_delegated_credentials_if_not_instance_of_identity() throws Exception {
    final TestingAuthenticationToken auth = new TestingAuthenticationToken("name", "password");
    auth.setDetails("not delegated credentials");
    when(security.getAuthentication()).thenReturn(auth);
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getName(), nullValue());
    assertThat(response.getSecret(), nullValue());
  }

  @Test
  public void should_include_identity_if_present() throws Exception {
    final TestingAuthenticationToken auth = new TestingAuthenticationToken("name", "password");
    auth.setDetails(new DelegatedCredentialsDetails(Identity.from("test-login", "test-secret")));
    when(security.getAuthentication()).thenReturn(auth);
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getName(), equalTo("test-login"));
    assertThat(response.getSecret(), equalTo("test-secret"));
  }

  @Test
  public void should_include_granted_authorities() throws Exception {
    when(security.getAuthentication())
        .thenReturn(new TestingAuthenticationToken("name", "password", "one", "two"));
    final AuthInfo response = subject.getAuthInfo();
    assertThat(response.getAuthorities(), containsInAnyOrder("one", "two"));
  }
}
