package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forward requests to the explorer pages of a container to the static content path. Requests to
 * the root {@code /favicon.ico} path are prefixed with the static content path.
 * <p>{@link javax.servlet.RequestDispatcher#forward(ServletRequest, ServletResponse) Forwarding} is a
 * container-internal redirect, i.e. it is not visible to the client and circumvents external
 * authentication.</p>
 */
@WebFilter(filterName = "explorer-redirect-filter", displayName = "explorer redirect"
    , description = "Internally redirect requests to explorer pages to the static content path"
    , urlPatterns = "/*"
    , dispatcherTypes = DispatcherType.REQUEST
    , asyncSupported = true
    , initParams = {})
public final class ExplorerPageRedirectFilter extends OncePerRequestFilter {
  private static final Logger log = getLogger(ExplorerPageRedirectFilter.class);

  /**
   * Create a redirect filter, which uses the given string as marker and redirect base path.
   */
  public static ExplorerPageRedirectFilter withStaticPath(final String marker) {
    if (!marker.startsWith("/")) {
      throw new IllegalArgumentException("illegal static redirect path <" + marker + ">");
    }
    return new ExplorerPageRedirectFilter(new ExplorerTemplate(marker));
  }

  private final ExplorerTemplate template;

  ExplorerPageRedirectFilter(final ExplorerTemplate template) {
    this.template = template;
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final FilterChain chain) throws ServletException, IOException {
    final String redirect = template.findRedirectTarget(request);
    final String original = request.getRequestURI();

    if (redirect == null) { // skip redirect
      log.debug(Scope.REQUEST.marker(), "not an explorer request ({}) - skip redirecting", original);
      chain.doFilter(request, response);
      return;
    }

    assert redirect.startsWith("/") : "redirect target is not an absolute path";
    final RequestDispatcher dispatcher = request.getRequestDispatcher(redirect);
    if (dispatcher == null) { // redirect cannot be handled
      final String message =
          Pretty.format("no handler for request to <%s> (redirected from <%s>) found", redirect, original);
      log.debug(Scope.REQUEST.marker(), message);
      if (!response.isCommitted()) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
      }
      return;
    }

    log.debug(Scope.REQUEST.marker(), "redirect request from {} to {}", original, redirect);
    dispatcher.forward(request, response);
  }

  static class ExplorerTemplate {
    static final String FAVICON = "/favicon.ico";

    private final UrlPathHelper paths;
    private final Pattern template;
    private final String favIconPath;

    public ExplorerTemplate(final String marker) {
      paths = new UrlPathHelper();
      paths.setUrlDecode(true);
      paths.setRemoveSemicolonContent(true);
      template = makePattern(marker);
      favIconPath = marker + FAVICON;
    }

    /**
     * Determine the correct redirect target for the given http request. If no redirect is required
     * return {@code null}.
     *
     * @param request http request
     * @return the redirect target or {@code null} if no redirect is required
     */
    String findRedirectTarget(final HttpServletRequest request) {
      final String path = paths.getPathWithinApplication(request);
      if (FAVICON.equals(path)) {
        return favIconPath;
      }
      final Matcher matcher = template.matcher(path);
      return matcher.matches() && headIsNotEmpty(matcher)
          ? matcher.group("target") : null;
    }

    // redirect would equal original path if head is empty
    private boolean headIsNotEmpty(final Matcher matcher) {
      final String head = matcher.group("head");
      return StringUtils.hasText(head);
    }

    private Pattern makePattern(final String marker) {
      final String regex = "^(?<head>.*?)(?<target>" + Pattern.quote(marker) + "(/.*)?)$";
      return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String toString() {
      return template.toString();
    }
  }

  @Override
  public String toString() {
    return "ExplorerPageRedirectFilter{" +
        "template=" + template +
        '}';
  }
}
