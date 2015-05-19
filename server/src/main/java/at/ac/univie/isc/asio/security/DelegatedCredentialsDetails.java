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

import static java.util.Objects.requireNonNull;

/**
 * Hold delegated credentials for authentication against other systems on behalf of the client and
 * an optional {@code AuthoritiesFilter} to apply additional restrictions on the request.
 */
public final class DelegatedCredentialsDetails {
  private final Identity credentials;

  /**
   * Store the given delegated credentials in addition to {@code WebAuthenticationDetails} functions.
   * By default no restrictions are applied.
   *
   * @param delegated delegated credentials
   */
  public DelegatedCredentialsDetails(final Identity delegated) {
    credentials = requireNonNull(delegated, "delegated credentials");
  }

  public Identity getCredentials() {
    return credentials;
  }
}
