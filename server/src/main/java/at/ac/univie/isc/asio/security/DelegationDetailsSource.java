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

import at.ac.univie.isc.asio.InvalidUsage;
import com.google.common.base.Converter;
import com.google.common.base.Objects;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.HttpServletRequest;

/**
 * Extract delegated credentials from the {@code 'Delegate-Authorization'} http header. Expects the
 * credentials to be given as defined in the {@code Basic Authentication} scheme.
 */
public final class DelegationDetailsSource
    implements AuthenticationDetailsSource<HttpServletRequest, DelegatedCredentialsDetails> {

  /**
   * Create a source, which reads the delegated credentials request header with the given name.
   */
  public static DelegationDetailsSource usingHeader(final String headerName) {
    return new DelegationDetailsSource(headerName, BasicAuthConverter.fromString());
  }

  private final Converter<String, Identity> parser;
  private final String delegatedCredentialsHeader;

  private DelegationDetailsSource(final String delegateAuthorizationHeader, final Converter<String, Identity> parser) {
    this.parser = parser;
    delegatedCredentialsHeader = delegateAuthorizationHeader;
  }

  public DelegatedCredentialsDetails buildDetails(final HttpServletRequest request) {
    final Identity credentials = findDelegatedCredentials(request);
    return new DelegatedCredentialsDetails(credentials);
  }

  private Identity findDelegatedCredentials(final HttpServletRequest request) {
    try {
      final String header = request.getHeader(delegatedCredentialsHeader);
      return Objects.firstNonNull(parser.convert(header), Identity.undefined());
    } catch (final InvalidUsage malformedCredentials) {
      throw new BadCredentialsException("illegal delegated credentials in '" + delegatedCredentialsHeader + "' header", malformedCredentials);
    }
  }
}
