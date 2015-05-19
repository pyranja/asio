/*
 * #%L
 * asio test
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio;

import javax.annotation.Nonnull;
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
   * @throws IllegalArgumentException if a variable has no value in {@code replacements}
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
}
