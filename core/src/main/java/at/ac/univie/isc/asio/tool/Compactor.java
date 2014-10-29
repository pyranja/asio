package at.ac.univie.isc.asio.tool;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Compact arbitrary strings to reduce their size, e.g. for logging.
 */
public abstract class Compactor implements Function<String, String> {
  /**
   * Removes all line breaks from the input string.
   */
  public static final Compactor REMOVE_LINE_BREAKS = new Compactor() {
    @Nonnull
    @Override
    public String apply(@Nullable final String input) {
      if (input == null) { return ""; }
      return input.replaceAll(Compactor.LINE_BREAKS, " ");
    }
  };

  /**
   * Whitespace is collapsed and trimmed, SPARQL PREFIX declarations are removed and the string is
   * truncated if it exceeds the {@link #MAX_LENGTH length limit}.
   */
  public static final Compactor TRIM = new Compactor() {
    @Nonnull
    @Override
    public String apply(@Nullable final String input) {
      if (input == null) { return ""; }
      final String reduced =
          input.replaceAll(LINE_BREAKS, " ").trim().replaceAll(WHITESPACE, " ").replaceAll(SPARQL_PREFIXES, "");
      if (reduced.length() > MAX_LENGTH) {
        return reduced.substring(0,
            MAX_LENGTH - TRUNCATION_MARKER.length()).concat(TRUNCATION_MARKER);
      } else {
        return reduced;
      }
    }
  };

  static final int MAX_LENGTH = 70;
  static final String TRUNCATION_MARKER = "...";
  private static final String LINE_BREAKS = "\\r\\n|\\r|\\n";
  private static final String WHITESPACE = "\\s+";
  private static final String SPARQL_PREFIXES = "(PREFIX (\\S+): <(\\S+)>\\s+)*";

  @Nonnull
  @Override
  public abstract String apply(@Nullable final String input);
}
