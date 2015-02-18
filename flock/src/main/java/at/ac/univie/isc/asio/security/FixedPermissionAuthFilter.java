package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.Scope;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Grant all requests a fixed {@link Role permission}, but extract and forward the VPH token.
 * Auth data is injected into the {@link javax.servlet.http.HttpServletRequest request context}.
 */
public final class FixedPermissionAuthFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(FixedPermissionAuthFilter.class);

  private final Role role;
  private final BasicAuthIdentityExtractor extractor;

  public FixedPermissionAuthFilter(final Role role, final BasicAuthIdentityExtractor extractor) {
    this.role = role;
    this.extractor = extractor;
  }

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final HttpServletResponse response = (HttpServletResponse) servletResponse;
    try {
      final AuthorizedRequestProxy authorizedRequest = authorize(request);
      chain.doFilter(authorizedRequest, servletResponse);
    } catch (BasicAuthIdentityExtractor.MalformedAuthHeader error) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, error.getMessage());
    }
  }

  private AuthorizedRequestProxy authorize(final HttpServletRequest request) {
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    final Identity identity = extractor.authenticate(authHeader);
    return AuthorizedRequestProxy.wrap(request, identity, role);
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    log.info(Scope.SYSTEM.marker(), "initialized with fixed permission {}", role);
  }

  @Override
  public void destroy() {
    log.info(Scope.SYSTEM.marker(), "shutting down");
  }
}
