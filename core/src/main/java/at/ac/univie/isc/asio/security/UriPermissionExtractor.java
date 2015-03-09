package at.ac.univie.isc.asio.security;

import com.google.common.base.Strings;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract permission string from a request URI, according to VPH spec :
 *  http://host:port/{common/prefix}/{permission}/{service/path}
 */
public final class UriPermissionExtractor implements FindAuthorization {
  /*
   * string template for the request URI matching pattern
   * the single parameter defines the common prefix which is to be ignored when parsing
   */
  public static final String URI_REGEX_TEMPLATE = "^%s/(?<permission>[^/?#]+)(?<tail>.*)?$";

  static Result result(final String permission, final String tail) {
    return Result.create(new SimpleGrantedAuthority(permission), tail);
  }

  private Pattern cachedParser;
  private String defaultPrefix;

  public UriPermissionExtractor() {
    cachePrefix(null);
  }

  public UriPermissionExtractor cachePrefix(@Nullable final String context) {
    defaultPrefix = Strings.nullToEmpty(context);
    cachedParser = makePattern(defaultPrefix);
    return this;
  }

  @Nonnull
  public Result accept(@Nullable final String uri, @Nullable final String context) throws VphUriRewriter.MalformedUri {
    if (uri == null) { throw new VphUriRewriter.MalformedUri("null"); }
    final Pattern parser = parserFor(context);
    final Matcher parsed = parser.matcher(uri);
    if (parsed.matches()) {
      final String permission = parsed.group("permission");
      final String tail = rootIfNull(Strings.emptyToNull(parsed.group("tail")));
      return result(permission, tail);
    } else {
      throw new VphUriRewriter.MalformedUri(uri);
    }
  }

  @Override
  public Result accept(final HttpServletRequest request) throws AuthenticationException {
    return accept(request.getRequestURI(), request.getContextPath());
  }

  private Pattern makePattern(final String prefix) {
    final String escaped = Pattern.quote(prefix);
    final String regex = String.format(Locale.ENGLISH, URI_REGEX_TEMPLATE, escaped);
    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  }

  private Pattern parserFor(final String prefix) {
    final String canonical = Strings.nullToEmpty(prefix);
    Pattern parser;
    if (defaultPrefix.equals(canonical)) {
      parser = cachedParser;
    } else {
      parser = makePattern(canonical);
    }
    return parser;
  }

  private String rootIfNull(final String maybeTail) {
    return maybeTail == null ? "/" : maybeTail;
  }

}
