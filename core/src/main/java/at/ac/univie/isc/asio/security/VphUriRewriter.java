package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * base, but assign {@link Role#NONE} to the request. Otherwise assume the request is targeting a
 * deployed schema with name {head} and redirect to {@code /CATALOG_PREFIX/{head}/{tail}}.
 */
@ThreadSafe
public final class VphUriRewriter implements FindAuthorization {
  /*
     * String template for the request URI matching pattern.
     * The single parameter defines the common prefix which is to be ignored when parsing.
     */
  static final String URI_REGEX_TEMPLATE =
      "^${context}/(?<head>[^/]+)/(?<authority>[^/?#]+)(?<tail>.*)?$";
  /**
   * String template for the redirection. Just concatenate prefix, head and tail to an absolute URI.
   */
  static final String CATALOG_REDIRECT = "${prefix}/${head}${tail}";
  /** Template for static content redirect to service base */
  static final String STATIC_CONTENT_REDIRECT = "/${tail}";

  /** Use name of {@link Role#NONE} as null authority */
  static final SimpleGrantedAuthority NO_AUTHORITY =
      new SimpleGrantedAuthority(Role.NONE.name());

  /**
   * Create a fresh instance, that uses the given prefixes.
   *
   * @param catalogPrefix prefix for redirects to schemas
   * @param staticPathPrefix prefix of static content request
   */
  public static VphUriRewriter withPrefixes(final String catalogPrefix, final String staticPathPrefix) {
    return new VphUriRewriter(catalogPrefix, staticPathPrefix);
  }

  private final LoadingCache<String, Pattern> parsers = CacheBuilder.newBuilder()
      .concurrencyLevel(1).initialCapacity(4).build(new CacheLoader<String, Pattern>() {
        @Override
        public Pattern load(@Nonnull final String key) throws Exception {
          return makePattern(key);
        }
      });

  private final String catalogPrefix;
  private final String staticPathPrefix;

  private VphUriRewriter(final String catalogPrefix, final String staticPathPrefix) {
    this.catalogPrefix = catalogPrefix;
    this.staticPathPrefix = staticPathPrefix.endsWith("/") ? staticPathPrefix : staticPathPrefix + "/";
  }

  @Override
  public Result accept(final HttpServletRequest request) throws AuthenticationException {
    final String uri = request.getRequestURI();
    if (uri == null) { throw new MalformedUri("null"); }
    final String normalizedContext = Strings.nullToEmpty(request.getContextPath());
    final Pattern parser = parsers.getUnchecked(normalizedContext);
    final Matcher parsed = parser.matcher(uri);
    if (parsed.matches()) {
      return selectRedirect(
          component("authority", parsed),
          component("head", parsed),
          component("tail", parsed)
      );
    } else {
      throw new MalformedUri(uri);
    }
  }

  private Result selectRedirect(final String authority, final String head, final String tail) {
    if (tail.startsWith(staticPathPrefix)) {
      final String staticContent = tail.substring(staticPathPrefix.length());
      final Map<String, String> args = ImmutableMap.of("tail", staticContent);
      return Result.create(NO_AUTHORITY, Pretty.substitute(STATIC_CONTENT_REDIRECT, args));
    } else {
      // TODO : check schema name here and omit /catalog prefix if schema not active
      final ImmutableMap<String, String> args = ImmutableMap.of("prefix", catalogPrefix, "head", head, "tail", tail);
      final String redirection = Pretty.substitute(CATALOG_REDIRECT, args);
      return Result.create(new SimpleGrantedAuthority(authority), redirection);
    }
  }

  private Pattern makePattern(final String prefix) {
    final String escaped = Pattern.quote(prefix);
    final Map<String, String> argument = Collections.singletonMap("context", escaped);
    final String regex = Pretty.substitute(URI_REGEX_TEMPLATE, argument);
    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  }

  private String component(final String name, final Matcher match) {
    final String component = match.group(name);
    assert component != null : "no match for group " + name;
    return component.toLowerCase(Locale.ENGLISH);
  }

  /**
   * Thrown if an {@link at.ac.univie.isc.asio.security.FindAuthorization} cannot parse input.
   */
  public static final class MalformedUri extends AuthenticationException {
    public MalformedUri(final String uri) {
      super("cannot extract authorization from request uri <" + uri + ">");
    }
  }
}
