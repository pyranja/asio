/*
 * #%L
 * asio common
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

import at.ac.univie.isc.asio.tool.TypedValue;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * The query language accepted by an endpoint.
 */
public final class Language extends TypedValue<String> {

  /**
   * Thrown if a requested {@link Language} is not supported.
   */
  public static final class NotSupported extends InvalidUsage {
    public NotSupported(final Language language) {
      super(language + " is not supported");
    }
  }

  public static final Language UNKNOWN = new Language("UNKNOWN");
  public static final Language SQL = new Language("SQL");
  public static final Language SPARQL = new Language("SPARQL");

  public static Language valueOf(final String value) {
    if (value == null || value.isEmpty()) {
      return UNKNOWN;
    } else {
      return new Language(value);
    }
  }

  Language(final String name) {
    super(name);
  }

  @Nonnull
  @Override
  protected String normalize(@Nonnull final String val) {
    return val.toUpperCase(Locale.ENGLISH).trim();
  }

  public String name() {
    return this.value();
  }
}
