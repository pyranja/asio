package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
  private final TranslateAuthorization adapter;

  public FixedPermissionAuthFilter(final Role role, final TranslateAuthorization adapter) {
    this.role = role;
    this.adapter = adapter;
  }

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final HttpServletResponse response = (HttpServletResponse) servletResponse;
    try {
      final TranslateAuthorization.Wrapped adapted = adapter.translate(role, request, response);
      chain.doFilter(adapted.request(), adapted.response());
    } catch (BasicAuthIdentityExtractor.MalformedAuthHeader error) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, error.getMessage());
    }
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
