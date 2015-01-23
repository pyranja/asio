package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.tool.ValueOrError;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract the operation from the request content type.
 */
class ExtractOperation {
  /** regex for direct command mime type */
  static final Pattern MEDIA_SUBTYPE_PATTERN = Pattern.compile("^(\\w+)-(\\w+)$");

  /**
   * Create an extractor.
   * @param expected of expected operation
   * @return the extractor
   */
  static ExtractOperation expect(final Language expected) {
    return new ExtractOperation(expected);
  }

  private final Language language;

  private ExtractOperation(final Language expected) {
    this.language = expected;
  }

  /**
   * Extract the operation from given media type if possible
   * @param content request content type
   * @return either the operation or a validation error
   */
  public ValueOrError<String> from(final MediaType content) {
    if (content == null) { return ValueOrError.invalid(missingTypeError()); }
    final Matcher match = MEDIA_SUBTYPE_PATTERN.matcher(content.getSubtype());
    if (!match.matches()) { return ValueOrError.invalid(malformedTypeError()); }
    if (!language.name().equalsIgnoreCase(match.group(1))) { return ValueOrError.invalid(languageMismatchError(language)); }
    return ValueOrError.valid(match.group(2));
  }

  private static NotSupportedException missingTypeError() {
    return new NotSupportedException("missing content type for direct operation");
  }

  private static NotSupportedException languageMismatchError(final Language language) {
    return new NotSupportedException(
        "illegal content type for direct operation - expected language " + language.name());
  }

  private static NotSupportedException malformedTypeError() {
    return new NotSupportedException("illegal content type for direct operation - use 'application/{language}-{operation}'");
  }
}
