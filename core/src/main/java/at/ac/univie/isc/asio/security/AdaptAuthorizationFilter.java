package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Adapt an incoming request from the external authorization mechanism (e.g. URI based) to an
 * internal one. If necessary, the request is internally forwarded to its true target.
 */
@WebFilter(filterName = "adapt-auth-filter", displayName = "adapt authorization filter"
    , description = "Adapt from external authorization mode to internal one"
    , urlPatterns = "/*"
    , dispatcherTypes = DispatcherType.REQUEST
    , asyncSupported = true
    , initParams = {})
public final class AdaptAuthorizationFilter implements Filter {
  private static final Logger log = LoggerFactory.getLogger(AdaptAuthorizationFilter.class);

  private final FindAuthorization authorizer;
  private final TranslateAuthorization translator;

  /**
   * servlet container constructor
   */
  @SuppressWarnings("UnusedDeclaration")
  public AdaptAuthorizationFilter() {
    this(new UriPermissionExtractor(), TranslateToServletContainerAuthorization.newInstance());
  }

  /**
   *
   * @param authorizer component that finds the authorization in the original request
   * @param translator component that translates request and response to internal mode
   */
  public AdaptAuthorizationFilter(final FindAuthorization authorizer, final TranslateAuthorization translator) {
    this.authorizer = authorizer;
    this.translator = translator;
  }

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final HttpServletResponse response = (HttpServletResponse) servletResponse;
    try {
      final FindAuthorization.AuthAndRedirect extracted = authorizer.accept(request);
      log.debug(Scope.REQUEST.marker(), "authorized request as {}", extracted.authority());
      final TranslateAuthorization.Wrapped authorized =
          translator.translate(extracted.authority(), request, response);
      if (extracted.redirection().isPresent()) {
        final RequestDispatcher dispatcher = findDispatcher(request, extracted.redirection().get());
        dispatcher.forward(authorized.request(), authorized.response());
      } else {
        log.debug(Scope.REQUEST.marker(), "forwarding request to <{}>", request.getRequestURI());
        chain.doFilter(authorized.request(), authorized.response());
      }
      log.debug(Scope.REQUEST.marker(), "request processing completed");
    } catch (final IllegalRedirect error) {
      log.debug(Scope.REQUEST.marker(), "no dispatcher found", error);
      sendErrorIfPossible(HttpServletResponse.SC_NOT_FOUND, error.getMessage(), response);
    } catch (final AuthenticationException | IllegalArgumentException error) {
      log.debug(Scope.REQUEST.marker(), "reject unauthorized request", error);
      sendErrorIfPossible(HttpServletResponse.SC_UNAUTHORIZED, error.getMessage(), response);
    } catch (final Throwable error) {
      log.warn(Scope.REQUEST.marker(), "uncaught error in filter chain", error);
      sendErrorIfPossible(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error.getMessage(), response);
    }
  }

  private void sendErrorIfPossible(final int statusCode,
                                   final String message,
                                   final HttpServletResponse response) throws IOException {
    if (!response.isCommitted()) {
      response.sendError(statusCode, message);
    }
  }

  private RequestDispatcher findDispatcher(final HttpServletRequest request, final String target) {
    log.debug(Scope.REQUEST.marker(), "redirecting from <{}> to <{}>", request.getRequestURI(), target);
    final RequestDispatcher dispatcher = request.getRequestDispatcher(target);
    if (dispatcher == null) {
      throw new IllegalRedirect(target, request.getRequestURI());
    }
    return dispatcher;
  }

  /**
   * internal error if no request dispatcher is found
   */
  static class IllegalRedirect extends RuntimeException {
    public IllegalRedirect(final String redirect, final String original) {
      super(Pretty.format("no handler for request to <%s> (redirected from <%s>) found", redirect, original));
    }
  }

  @Override
  public void init(final FilterConfig config) throws ServletException {
    log.info(Scope.SYSTEM.marker(),
        "initialized with finder <{}> and translator <{}>", authorizer, translator);
  }

  @Override
  public void destroy() {
    log.info(Scope.SYSTEM.marker(), "shutting down");
  }
}
