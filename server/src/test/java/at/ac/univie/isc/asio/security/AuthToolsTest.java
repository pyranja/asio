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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class AuthToolsTest {

  private final SecurityContextImpl context = new SecurityContextImpl();

  @Test
  public void should_find_identity_in_authentication_details() throws Exception {
    final TestingAuthenticationToken authentication = new TestingAuthenticationToken("test", "password");
    authentication.setDetails(new DelegatedCredentialsDetails(Identity.from("user", "password")));
    context.setAuthentication(authentication);
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.from("user", "password")));
  }

  @Test
  public void should_yield_undefined_if_context_empty() throws Exception {
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.undefined()));
  }

  @Test
  public void should_yield_undefined_if_authentication_has_no_details() throws Exception {
    context.setAuthentication(new TestingAuthenticationToken("user", "password"));
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.undefined()));
  }

  @Test
  public void should_yield_undefined_if_unexpected_details_type_found() throws Exception {
    final TestingAuthenticationToken authentication = new TestingAuthenticationToken("test", "password");
    authentication.setDetails("no delegated credentials");
    context.setAuthentication(authentication);
    assertThat(AuthTools.findIdentity(context), equalTo(Identity.undefined()));
  }
}
