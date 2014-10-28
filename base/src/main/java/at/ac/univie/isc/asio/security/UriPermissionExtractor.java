package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.tool.Pair;
import com.google.common.base.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract permission string from a request URI, according to VPH spec :
 *  http://host:port/{common/prefix}/{permission}/{service/path}
 */
public final class UriPermissionExtractor {
  /*
   * string template for the request URI matching pattern
   * the single parameter defines the common prefix which is to be ignored when parsing
   */
  public static final String URI_REGEX_TEMPLATE = "^%s/(?<permission>[^/?#]+)(?<tail>.*)?$";

  static Result result(final String permission, final String tail) {
    return new Result(permission, tail);
  }

  private Pattern cachedParser;
  private String defaultPrefix;

  public UriPermissionExtractor() {
    cachePrefix(null);
  }

  public UriPermissionExtractor cachePrefix(@Nullable final String prefix) {
    defaultPrefix = Strings.nullToEmpty(prefix);
    cachedParser = makePattern(defaultPrefix);
    return this;
  }

  /**
   * Decompose given relative {@code uri} into prefix, permission and tail parts.
   * @param uri complete request uri
   * @param prefix to be ignored when parsing
   * @return A pair of {@code permission} and {@code tail}
   */
  @Nonnull
  public Result accept(@Nullable final String uri, @Nullable final String prefix) throws MalformedUri {
    if (uri == null) { throw new MalformedUri("null"); }
    final Pattern parser = parserFor(prefix);
    final Matcher parsed = parser.matcher(uri);
    if (parsed.matches()) {
      final String permission = parsed.group("permission");
      final String tail = rootIfNull(Strings.emptyToNull(parsed.group("tail")));
      return result(permission, tail);
    } else {
      throw new MalformedUri(uri);
    }
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

  public static final class MalformedUri extends DatasetUsageException {
    public MalformedUri(final String uri) {
      super("cannot extract permission from request uri <" + uri +">");
    }
  }

  static final class Result extends Pair<String, String> {
    private Result(final String permission, final String tail) {
      super(permission, tail);
    }

    /**
     * @return raw permission value, without {@code /} slashes.
     */
    public final String permission() {
      return first();
    }

    /**
     * @return an absolute URI, including everything following directly after permission
     */
    public final String tail() {
      return second();
    }
  }
}
