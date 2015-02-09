package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.security.Authorizer;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.SecurityContextHolder;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Optional;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.SecurityContext;

/**
 * Authorize based on the current {@link javax.ws.rs.core.SecurityContext}.
 */
public final class JaxrsAuthorizer implements Authorizer {
  private JaxrsAuthorizer() {
  }

  public static JaxrsAuthorizer create() {
    return new JaxrsAuthorizer();
  }

  @Override
  public void check(final Invocation invocation) throws RuntimeException {
    final Optional<SecurityContext> security = SecurityContextHolder.get();
    final Role required = invocation.requires();
    if (!security.isPresent()) {
      throw new IllegalStateException("no security context available");
    } else if (!security.get().isUserInRole(required.name())) {
      throw new ForbiddenException(Pretty.format("requires %s rights", required));
    }
  }
}
