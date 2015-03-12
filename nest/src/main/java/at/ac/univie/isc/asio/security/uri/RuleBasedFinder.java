package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import com.google.common.collect.ImmutableList;
import org.springframework.security.core.AuthenticationException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Base implementation that handles parsing of request URI and delegating work to a matching
 * {@link UriAuthRule}.
 */
public final class RuleBasedFinder implements FindAuthorization {
  private final List<UriAuthRule> rules;
  private final UriParser parser;

  public static RuleBasedFinder create(@Nonnull final String uriRegex, @Nonnull final UriAuthRule... rules) {
    return new RuleBasedFinder(UriParser.create(uriRegex), ImmutableList.copyOf(rules));
  }

  RuleBasedFinder(@Nonnull final UriParser parser, @Nonnull final List<UriAuthRule> rules) {
    this.parser = requireNonNull(parser);
    this.rules = requireNonNull(rules);
  }

  @Override
  public AuthAndRedirect accept(final HttpServletRequest request) throws AuthenticationException {
    final UriAuthRule.PathElements path =
        parser.parse(request.getRequestURI(), request.getContextPath());
    for (UriAuthRule rule : rules) {
      if (rule.canHandle(path)) {
        return rule.handle(path);
      }
    }
    throw new NoAuthRuleFound(request.getRequestURI());
  }

  public static class NoAuthRuleFound extends AuthenticationException {
    public NoAuthRuleFound(final String uri) {
      super("no authorization rule found for <" + uri + ">");
    }
  }

  @Override
  public String toString() {
    return "RuleBasedFinder{" + "rules=" + rules + ", parser=" + parser + '}';
  }
}
