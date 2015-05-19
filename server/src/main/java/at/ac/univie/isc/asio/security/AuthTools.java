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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * Tools for working with spring security.
 */
public final class AuthTools {
  private AuthTools() { /* no instances */ }

  /**
   * Extract the {@link Identity} from the authentication's details if present.
   *
   * @param context spring security context
   * @return the client's identity if one is present
   */
  @Nonnull
  public static Identity findIdentity(@Nonnull final SecurityContext context) {
    requireNonNull(context, "spring security context");
    final Authentication authentication = context.getAuthentication();
    if (authentication != null && authentication.getDetails() instanceof DelegatedCredentialsDetails) {
      final DelegatedCredentialsDetails details =
          (DelegatedCredentialsDetails) authentication.getDetails();
      return details.getCredentials();
    }
    return Identity.undefined();
  }
}
