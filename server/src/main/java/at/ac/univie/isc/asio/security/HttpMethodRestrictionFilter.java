package at.ac.univie.isc.asio.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Apply a restriction on the granted authorities based on the HTTP method.
 * The restriction is applied by copying the current authentication from the security context,
 * but filtering the granted authorities to exclude the restricted ones.
 */
public final class HttpMethodRestrictionFilter extends GenericFilterBean {
  /**
   * excludes modifying authorities from GET requests
   */
  static final FilterAuthorities RESTRICTION =
      FilterAuthorities.exclude(Collections.<GrantedAuthority>singletonList(Permission.INVOKE_UPDATE));

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) servletRequest;
    final Authentication authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && HttpMethod.GET.name().equalsIgnoreCase(request.getMethod())) {
      logger.debug("applying " + RESTRICTION + " to " + authentication);
      Set<GrantedAuthority> restricted = RESTRICTION.mapAuthorities(authentication.getAuthorities());
      if (restricted.isEmpty()) { // anonymous and remember me tokens require at least one authority
        restricted = Collections.<GrantedAuthority>singleton(Role.NONE);
      }
      if (!restricted.containsAll(authentication.getAuthorities())) {
        final AbstractAuthenticationToken replacement = copy(authentication, restricted);
        replacement.setDetails(authentication.getDetails());
        logger.debug("injecting " + replacement);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(replacement);
      } else {
        logger.debug("skip restricting " + authentication + " as it contains no restricted authorities");
      }
    } else {
      logger.debug("skip restricting " + authentication + " on HTTP method " + request.getMethod());
    }
    chain.doFilter(request, response);
  }

  /**
   * Copy the original authentication, but use the restricted set of authorities. Keep special token
   * classes, like Anonymous, RememberMe, etc. .
   */
  private AbstractAuthenticationToken copy(final Authentication authentication, final Set<GrantedAuthority> restricted) {
    final AbstractAuthenticationToken replacement;
    if (authentication instanceof AnonymousAuthenticationToken) {
      replacement =
          new AnonymousAuthenticationToken("dummy-key", authentication.getPrincipal(), restricted);
    } else if (authentication instanceof RememberMeAuthenticationToken) {
      replacement =
          new RememberMeAuthenticationToken("dummy-key", authentication.getPrincipal(), restricted);
    } else if (authentication instanceof PreAuthenticatedAuthenticationToken) {
      replacement =
          new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), restricted);
    } else {
      replacement =
          new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), restricted);
    }
    return replacement;
  }
}
