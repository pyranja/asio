package at.ac.univie.isc.asio.tool;

import com.google.common.base.CharMatcher;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public final class Pretty {

  private Pretty() { /* static */ }

  /**
   * Pad the given string to justify it.
   * @param text to be justified
   * @param minLength pad until this length is reached
   * @param pad char to use as padding
   * @return justified string
   */
  @Nonnull
  public static String justify(@Nonnull final String text, final int minLength, final char pad) {
    requireNonNull(text);
    if (minLength < 0) { throw new IllegalArgumentException("minLength must be positive"); }
    if (text.length() >= minLength) { return text; }
    final StringBuilder buffer = new StringBuilder(minLength);
    final int padLength = minLength - text.length();
    for (int i = padLength / 2; i > 0; i--) {
      buffer.append(pad);
    }
    buffer.append(text);
    for (int i = padLength / 2 + padLength % 2; i > 0; i--) {
      buffer.append(pad);
    }
    return buffer.toString();
  }

  /**
   * Format the given string with fixed {@link java.util.Locale#ENGLISH english locale}.
   *
   * @see String#format(String, Object...)
   * @param template a format string
   * @param arguments format arguments
   * @return formatted string
   */
  @Nonnull
  public static String format(String template, final Object... arguments) {
    return String.format(Locale.ENGLISH, template, arguments);
  }

  private static final CharMatcher LINE_BREAKS = CharMatcher.anyOf("\r\n").precomputed();

  private static final char SPACE = ' ';

  /**
   * Remove all line breaks from the input string and collapse whitespaces.
   *
   * @param input the string that should be compacted
   * @return a copy of the string without line breaks and whitespace collapsed
   */
  @Nonnull
  public static String compact(final String input) {
    return CharMatcher.WHITESPACE.trimAndCollapseFrom(LINE_BREAKS.replaceFrom(input, SPACE), SPACE);
  }

  private static final Pattern TOKEN = Pattern.compile("\\$\\{(.*?)\\}");

  /**
   * Substitute all <code>${variable-name}</code> style variables in the given string with the
   * corresponding values from the given map. Replacement values are converted to their
   * {@link Object#toString() default string representation} before substitution.
   * If no replacement value is found for a variable or a variable token has no content,
   * an error is thrown.
   * @param template text with embedded variables
   * @param replacements map of substitution values
   * @return a string with all matched variables substituted
   * @throws java.lang.IllegalArgumentException if a variable has no value in {@code replacements}
   *  or a variable token is empty (<code>${}</code>).
   */
  public static String substitute(final String template, final Map<String, ?> replacements) {
    requireNonNull(template, "template input");
    requireNonNull(replacements, "replacement map");
    final Matcher matcher = TOKEN.matcher(template);
    final StringBuffer buffer = new StringBuffer(template.length());
    while (matcher.find()) {
      final String key = matcher.group(1);
      if (key.isEmpty()) { throw new IllegalArgumentException("empty variable token found in "+ template); }
      final Object found = replacements.get(key);
      if (found == null) { throw new IllegalArgumentException("missing replacement for key "+ key); }
      final String substitute = Matcher.quoteReplacement(Objects.toString(found));
      matcher.appendReplacement(buffer, substitute);
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  /**
   * Expand the given {@link javax.xml.namespace.QName qualified name} by concatenating the
   * namespace uri and the local part.
   *
   * @param qname a qualified name
   * @return expanded form of the given name
   */
  public static String expand(final QName qname) {
    requireNonNull(qname, "qualified name");
    return qname.getNamespaceURI() + qname.getLocalPart();
  }
}
