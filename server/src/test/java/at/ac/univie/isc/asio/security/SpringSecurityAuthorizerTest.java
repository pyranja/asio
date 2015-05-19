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

import at.ac.univie.isc.asio.engine.Invocation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;

import static org.mockito.Mockito.when;

public class SpringSecurityAuthorizerTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final SecurityContext security = Mockito.mock(SecurityContext.class);
  private final Invocation invocation = Mockito.mock(Invocation.class);

  private final SpringSecurityAuthorizer subject = new SpringSecurityAuthorizer(security);

  @Test
  public void should_allow_invocation_with_sufficient_permission() throws Exception {
    when(security.getAuthentication()).thenReturn(auth(Permission.INVOKE_QUERY));
    when(invocation.requires()).thenReturn(Permission.INVOKE_QUERY);
    subject.check(invocation);
  }

  @Test
  public void should_reject_if_required_permission_not_granted() throws Exception {
    when(security.getAuthentication()).thenReturn(auth(Permission.INVOKE_QUERY));
    when(invocation.requires()).thenReturn(Permission.INVOKE_UPDATE);
    error.expect(AccessDeniedException.class);
    subject.check(invocation);
  }

  @Test
  public void fail_fast_on_null_authentication() throws Exception {
    error.expect(IllegalStateException.class);
    subject.check(invocation);
  }

  private TestingAuthenticationToken auth(final Permission... permissions) {
    final String[] authorities = new String[permissions.length];
    for (int i = 0; i < permissions.length; i++) {
      authorities[i] = permissions[i].toString();
    }
    return new TestingAuthenticationToken("test-user", "test-password", authorities);
  }
}
