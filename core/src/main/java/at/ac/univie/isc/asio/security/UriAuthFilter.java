package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.config.AsioConfiguration;
import com.google.common.base.Optional;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("UnusedDeclaration")
@WebFilter(filterName = "auth-filter", displayName = "Uri Auth Filter"
    , description = "Authorize based on the request URI"
    , urlPatterns = "/*"
    , dispatcherTypes = DispatcherType.REQUEST
    , asyncSupported = true
    , initParams = {})
public final class UriAuthFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(UriAuthFilter.class);

  private UriPermissionExtractor parser;
  private VphTokenExtractor extractor;

  /** servlet container constructor */
  public UriAuthFilter() {
    this(new UriPermissionExtractor(), new VphTokenExtractor());
  }

  public UriAuthFilter(final UriPermissionExtractor parser, final VphTokenExtractor extractor) {
    this.parser = parser;
    this.extractor = extractor;
  }

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final HttpServletResponse response = (HttpServletResponse) servletResponse;
    try {
      authorize(request, response);
    } catch (UriPermissionExtractor.MalformedUri | VphTokenExtractor.MalformedAuthHeader | IllegalArgumentException error) {
      log.debug("reject request : {}", error.getMessage()); // TODO : log to access.log -> new marker
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, error.getMessage());
    }
  }

  private void authorize(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    final UriPermissionExtractor.Result extracted =
        parser.accept(request.getRequestURI(), request.getContextPath());
    final Permission permission = Permission.parse(extracted.permission());
    final Token user = extractToken(request);
    final PermissionProxyRequest authorizedRequest =
        new PermissionProxyRequest(request, user, permission);
    log.debug("authorized {} with permission {} and redirecting to <{}>", user, permission, extracted.tail());
    final RequestDispatcher dispatcher = request.getRequestDispatcher(extracted.tail());
    dispatcher.forward(authorizedRequest, response);
  }

  private Token extractToken(final HttpServletRequest request) {
    final Optional<String> authHeader =
        Optional.fromNullable(request.getHeader(HttpHeaders.AUTHORIZATION));
    return extractor.authenticate(authHeader);
  }

  @Override
  public void init(final FilterConfig config) throws ServletException {
    final String contextPath = config.getServletContext().getContextPath();
    parser.cachePrefix(contextPath);
    log.info(AsioConfiguration.SYSTEM, "initialized on context <{}>", contextPath);
  }

  @Override
  public void destroy() {
    log.info(AsioConfiguration.SYSTEM, "shutting down");
  }

  private static class PermissionProxyRequest extends HttpServletRequestWrapper {
    private final Token user;
    private final Permission permission;

    public PermissionProxyRequest(final HttpServletRequest request, final Token user, final Permission permission) {
      super(request);
      this.user = user;
      this.permission = permission;
    }

    @Override
    public Token getUserPrincipal() {
      return user;
    }

    @Override
    public String getRemoteUser() {
      return user.getName();
    }

    @Override
    public String getAuthType() {
      return HttpServletRequest.BASIC_AUTH;
    }

    @Override
    public boolean isUserInRole(final String role) {
      assert role != null : "got null role";
      return permission.grants(Role.valueOf(role));
    }
  }
}
