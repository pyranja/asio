package at.ac.univie.isc.asio.security;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Authenticate the user by {@link at.ac.univie.isc.asio.security.VphTokenExtractor#authenticate(javax.ws.rs.core.MultivaluedMap) parsing}
 * the VPH auth token from the request. Authorize by examining the {@code permission} path parameter.
 * Auth information is published on the requests {@link javax.ws.rs.core.SecurityContext}.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public final class VphAuthFilter implements ContainerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(VphAuthFilter.class);

  private final VphTokenExtractor security;

  public VphAuthFilter(final VphTokenExtractor security) {
    this.security = security;
  }

  @Override
  public void filter(final ContainerRequestContext context) throws IOException {
    final Token user = security.authenticate(context.getHeaders());
    log.trace("found user : {}", user);
    final Permission permission = extractUriPermission(context.getUriInfo());
    log.trace("found permission : {}", permission);
    final ImmutableSecurityContext securityContext = new ImmutableSecurityContext(user, permission);
    context.setSecurityContext(securityContext);
    log.info("authorization : {}", securityContext);
  }

  private static final String PERMISSION_PARAMETER_KEY = "permission";

  private Permission extractUriPermission(final UriInfo uriInfo) {
    final List<String> params = uriInfo.getPathParameters().get(PERMISSION_PARAMETER_KEY);
    final Permission result;
    if (params == null || params.size() != 1) {
      log.warn("expected exactly one but found {} permission path parameters", params.size());
      result = Permission.NONE;
    } else {
      result = Permission.parse(Iterables.getOnlyElement(params));
    }
    return result;
  }

  @Immutable
  private static final class ImmutableSecurityContext implements SecurityContext {
    private final Token user;
    private final Permission permission;

    private ImmutableSecurityContext(final Token user, final Permission permission) {
      this.user = user;
      this.permission = permission;
    }

    @Override
    public Token getUserPrincipal() {
      return user;
    }

    @Override
    public boolean isUserInRole(final String roleName) {
      requireNonNull(roleName, "cannot check null role");
      return permission.grants(Role.valueOf(roleName));
    }

    @Override
    public boolean isSecure() {
      return false;
    }

    @Override
    public String getAuthenticationScheme() {
      return SecurityContext.BASIC_AUTH;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("user", user)
          .add("permission", permission)
          .toString();
    }
  }
}
