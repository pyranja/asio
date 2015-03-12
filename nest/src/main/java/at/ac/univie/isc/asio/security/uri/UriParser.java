package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.AuthenticationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Parse servlet request URIs according to a configurable regular expression.
 */
/* final */ class UriParser {
  /**
   * Template for combining the regex URI template with a fixed content.
   */
  private static final String REGEX_TEMPLATE = "^${context}${uri-template}$";

  private final LoadingCache<String, Pattern> patterns = CacheBuilder.newBuilder()
      .concurrencyLevel(1).initialCapacity(4).build(new CacheLoader<String, Pattern>() {
        @Override
        public Pattern load(@Nonnull final String key) throws Exception {
          return makePattern(key);
        }
      });

  private final String template;

  private UriParser(@Nonnull final String regex) {
    template = requireNonNull(regex);
  }

  /**
   * Create a parser, that uses the given regular expression to match input URIs. The given regex
   * <strong>must not</strong> contain the bounding operators {@code '^'} or {@code '$'}.
   *
   * @param regex for uri matching
   * @return initialized parser
   */
  public static UriParser create(final String regex) {
    return new UriParser(regex);
  }

  /**
   * Parse the given request URI, but ignore the context if it is not null. The returned match will
   * hold all matched groups from the set template.
   *
   * @param uri    uri that should be decomposed
   * @param prefix prefix that will be ignored
   * @return match results containing any matched group
   */
  @Nonnull
  public UriAuthRule.PathElements parse(@Nonnull final String uri, @Nullable final String prefix) {
    if (uri == null) { throw new MalformedUri(uri, "illegal uri"); }
    final Pattern pattern = patterns.getUnchecked(Strings.nullToEmpty(prefix));
    final Matcher parsed = pattern.matcher(uri);
    if (parsed.matches()) {
      return new Match(parsed);
    } else {
      throw new MalformedUri("did not match <" + pattern + ">", uri);
    }
  }

  private Pattern makePattern(final String prefix) {
    final String escaped = Pattern.quote(prefix);
    final Map<String, String> arguments =
        ImmutableMap.of("context", escaped, "uri-template", template);
    final String regex = Pretty.substitute(REGEX_TEMPLATE, arguments);
    return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Hold matched components from a parsed URI.
   */
  public static final class Match implements UriAuthRule.PathElements {
    private final Matcher matcher;

    Match(final Matcher matcher) {
      this.matcher = matcher;
    }

    @Override
    public String require(final String key) {
      final String group = matcher.group(key);
      if (group == null) {
        throw new IllegalArgumentException(
            "group <" + key + "> not matched in <" + matcher.group(0) + ">");
      }
      return group.toLowerCase(Locale.ENGLISH);
    }
  }

  /**
   * Thrown if a request uri does not match expectations.
   */
  public static final class MalformedUri extends AuthenticationException {
    public MalformedUri(final String reason, final String uri) {
      super("failed to match <" + uri + "> - " + reason);
    }
  }

  @Override
  public String toString() {
    return "{" + "template='" + template + '\'' + '}';
  }
}
