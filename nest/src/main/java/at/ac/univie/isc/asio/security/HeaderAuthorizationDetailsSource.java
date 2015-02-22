package at.ac.univie.isc.asio.security;

import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.Attributes2GrantedAuthoritiesMapper;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Extract {@link org.springframework.security.core.GrantedAuthority granted authorities} from a
 * HTTP request header. The request header value is converted to authorities using the configured
 * mapper.
 */
public final class HeaderAuthorizationDetailsSource
    implements AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> {
  private static final Logger log = getLogger(HeaderAuthorizationDetailsSource.class);

  private final String authorizationHeaderName;
  private final Attributes2GrantedAuthoritiesMapper grantMapper;
  private final GetMethodRestriction restriction;

  public HeaderAuthorizationDetailsSource(final String authorizationHeaderName, final Attributes2GrantedAuthoritiesMapper grantMapper, final GetMethodRestriction restriction) {
    this.restriction = requireNonNull(restriction);
    this.authorizationHeaderName = requireNonNull(authorizationHeaderName);
    this.grantMapper = requireNonNull(grantMapper);
  }

  @Override
  public PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails buildDetails(final HttpServletRequest context) {
    final String header = context.getHeader(authorizationHeaderName);
    if (header == null) {
      return emptyDetails(context);
    } else {
      log.debug("Authorization header '{}' found with content '{}'", authorizationHeaderName, header);
      final List<String> attributes = Arrays.asList(header.split(","));
      final Collection<? extends GrantedAuthority> grantedAuthorities = grantMapper.getGrantedAuthorities(attributes);
      final Collection<GrantedAuthority> filteredAuthorities = restriction.filter(grantedAuthorities, context);
      log.debug("Authorization header attributes {} mapped to authorities {}", header.split(","), grantedAuthorities);
      return authorized(context, filteredAuthorities);
    }
  }

  private PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails authorized(final HttpServletRequest context, final Collection<? extends GrantedAuthority> grantedAuthorities) {
    return new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(context, grantedAuthorities);
  }

  private PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails emptyDetails(final HttpServletRequest context) {
    return authorized(context, AuthorityUtils.NO_AUTHORITIES);
  }
}
