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
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Decide based on spring SecurityContext.
 */
@Component
public final class SpringSecurityAuthorizer implements Authorizer {
  private static final Logger log = getLogger(SpringSecurityAuthorizer.class);

  private final SecurityContext context;

  @Autowired
  public SpringSecurityAuthorizer(final SecurityContext security) {
    context = requireNonNull(security);
  }

  @Override
  public void check(final Invocation invocation) throws RuntimeException {
    final Permission required = invocation.requires();
    final Authentication authentication = fetchAndValidateAuthentication();
    for (final GrantedAuthority granted : authentication.getAuthorities()) {
      if (required.name().equals(granted.getAuthority())) {
        return;
      }
    }
    log.debug("not authorized to invoke - required {} - granted {}", required, authentication.getAuthorities());
    throw new AccessDeniedException(Pretty.format("requires %s rights", required));
  }

  private Authentication fetchAndValidateAuthentication() {
    final Authentication authentication = context.getAuthentication();
    if (authentication == null) {
      log.warn("found null authentication - security config broken?");
      throw new IllegalStateException("missing authentication");
    }
    return authentication;
  }
}
