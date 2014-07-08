package at.ac.univie.isc.asio.tool;

import com.google.common.base.Function;

import javax.annotation.Nullable;

public final class CommandShortener implements Function<String, String> {
  public static final CommandShortener INSTANCE = new CommandShortener();

  public static final int MAX_LENGTH = 70;
  public static final String TRUNCATION_MARKER = "...";

  private final String LINE_BREAKS = "\\r\\n|\\r|\\n";
  private final String WHITESPACE = "\\s+";
  private final String SPARQL_PREFIXES = "(PREFIX (\\S+): <(\\S+)>\\s+)*";

  @Nullable
  @Override
  public String apply(@Nullable final String input) {
    final String reduced = input.replaceAll(LINE_BREAKS, " ").trim().replaceAll(WHITESPACE, " ").replaceAll(SPARQL_PREFIXES, "");
    if (reduced.length() > MAX_LENGTH) {
      return reduced.substring(0, MAX_LENGTH - TRUNCATION_MARKER.length()).concat(TRUNCATION_MARKER);
    } else {
      return reduced;
    }
  }
}
