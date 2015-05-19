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

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import java.security.Principal;

import static org.junit.Assert.assertThat;

public class DelegationDetailsSourceTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final DelegationDetailsSource subject =
      DelegationDetailsSource.usingHeader("Delegate-Authorization");
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  public void should_set_undefined_credentials_if_header_missing() throws Exception {
    final DelegatedCredentialsDetails details = subject.buildDetails(request);
    assertThat(details.getCredentials(), eq(Identity.undefined()));
  }

  @Test
  public void should_inject_credentials_contained_in_auth_header() throws Exception {
    final String header = "Basic " + BaseEncoding.base64().encode("user:password".getBytes(Charsets.UTF_8));
    request.addHeader("Delegate-Authorization", header);
    final DelegatedCredentialsDetails details = subject.buildDetails(request);
    assertThat(details.getCredentials(), eq(Identity.from("user", "password")));
  }

  @Test
  public void should_fail_fast_on_malformed_auth_header() throws Exception {
    request.addHeader("Delegate-Authorization", "IllegalScheme ABCDE");
    error.expect(BadCredentialsException.class);
    subject.buildDetails(request);
  }

  private Matcher<Principal> eq(final Identity expected) {
    return Matchers.<Principal>is(expected);
  }
}
