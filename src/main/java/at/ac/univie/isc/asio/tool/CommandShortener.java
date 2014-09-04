package at.ac.univie.isc.asio.tool;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CommandShortener implements Function<String, String> {
  static final String LINE_BREAKS = "\\r\\n|\\r|\\n";
  private final String WHITESPACE = "\\s+";
  private final String SPARQL_PREFIXES = "(PREFIX (\\S+): <(\\S+)>\\s+)*";

  public static final Function<String, String> REMOVE_LINE_BREAKS =
      new Function<String, String>() {
        @Nonnull
        @Override
        public String apply(@Nullable final String input) {
          assert input != null;
          return input.replaceAll(CommandShortener.LINE_BREAKS, " ");
        }
      };

  public static final CommandShortener INSTANCE = new CommandShortener();

  public static final int MAX_LENGTH = 70;
  public static final String TRUNCATION_MARKER = "...";

  @Nullable
  @Override
  public String apply(@Nullable final String input) {
    assert input != null;
    final String reduced = input.replaceAll(LINE_BREAKS, " ").trim().replaceAll(WHITESPACE, " ").replaceAll(SPARQL_PREFIXES, "");
    if (reduced.length() > MAX_LENGTH) {
      return reduced.substring(0, MAX_LENGTH - TRUNCATION_MARKER.length()).concat(TRUNCATION_MARKER);
    } else {
      return reduced;
    }
  }
}
