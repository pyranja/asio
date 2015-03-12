package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Parse an URI with embedded authorization information. The expected URI pattern is as follows:
 * <pre>   {prefix}/{head}/{role}/{tail}</pre>
 * where
 * <ul>
 * <li>{@code prefix} is omitted, e.g. the servlet context</li>
 * <li>{@code head} is the requested schema</li>
 * <li>{@code role} is the granted authority</li>
 * <li>{@code tail} is anything following the role component (paths, query string, fragments)</li>
 * </ul>
 * Return the extracted role and an absolute redirection path without the prefix and role component.
 * If the {tail} component starts with the configured static path prefix, redirect to the service
 * base, but assign {@link at.ac.univie.isc.asio.security.Role#NONE} to the request. Otherwise assume the request is targeting a
 * deployed schema with name {head} and redirect to {@code /CATALOG_PREFIX/{head}/{tail}}.
 */
@ThreadSafe
public final class VphUriRewriter implements FindAuthorization {
  /*
     * String template for the request URI matching pattern.
     * The single parameter defines the common prefix which is to be ignored when parsing.
     */
  static final String URI_REGEX_TEMPLATE = "/(?<head>[^/]+)/(?<authority>[^/?#]+)(?<tail>.*)";
  /**
   * String template for the redirection. Just concatenate prefix, head and tail to an absolute URI.
   */
  static final String CATALOG_REDIRECT = "${prefix}/${head}${tail}";
  /**
   * Template for static content redirect to service base
   */
  static final String STATIC_CONTENT_REDIRECT = "/${tail}";

  /**
   * Use name of {@link at.ac.univie.isc.asio.security.Role#NONE} as null authority
   */
  static final SimpleGrantedAuthority NO_AUTHORITY =
      new SimpleGrantedAuthority(Role.NONE.name());

  /**
   * Create a fresh instance, that uses the given prefixes.
   *
   * @param catalogPrefix    prefix for redirects to schemas
   * @param staticPathPrefix prefix of static content request
   */
  public static VphUriRewriter withPrefixes(final String catalogPrefix, final String staticPathPrefix) {
    return new VphUriRewriter(catalogPrefix, staticPathPrefix);
  }

  private final UriParser parser = UriParser.create(URI_REGEX_TEMPLATE);
  private final String catalogPrefix;
  private final String staticPathPrefix;

  private VphUriRewriter(final String catalogPrefix, final String staticPathPrefix) {
    this.catalogPrefix = catalogPrefix;
    this.staticPathPrefix =
        staticPathPrefix.endsWith("/") ? staticPathPrefix : staticPathPrefix + "/";
  }

  @Override
  public AuthAndRedirect accept(final HttpServletRequest request) throws AuthenticationException {
    final UriAuthRule.PathElements
        result = parser.parse(request.getRequestURI(), request.getContextPath());
    return selectRedirect(result.require("authority"), result.require("head"), result.require("tail"));
  }

  private AuthAndRedirect selectRedirect(final String authority, final String head, final String tail) {
    if (tail.startsWith(staticPathPrefix)) {
      final String staticContent = tail.substring(staticPathPrefix.length());
      final Map<String, String> args = ImmutableMap.of("tail", staticContent);
      return AuthAndRedirect.create(NO_AUTHORITY, Pretty.substitute(STATIC_CONTENT_REDIRECT, args));
    } else {
      // TODO : check schema name here and omit /catalog prefix if schema not active
      final ImmutableMap<String, String> args =
          ImmutableMap.of("prefix", catalogPrefix, "head", head, "tail", tail);
      final String redirection = Pretty.substitute(CATALOG_REDIRECT, args);
      return AuthAndRedirect.create(new SimpleGrantedAuthority(authority), redirection);
    }
  }
}
