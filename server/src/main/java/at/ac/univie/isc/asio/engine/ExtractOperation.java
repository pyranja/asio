/*
 * #%L
 * asio server
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
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;
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
