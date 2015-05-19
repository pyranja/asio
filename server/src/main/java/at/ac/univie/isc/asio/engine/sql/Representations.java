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
package at.ac.univie.isc.asio.engine.sql;

import com.google.common.io.BaseEncoding;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory for datatype representations.
 * <p>The {@code xs*} family of representation is compliant with the canonical representations of
 * <a href="http://www.w3.org/TR/xmlschema-2/">XML Schema Datatypes</a>.</p>
 * <p>{@link Representation} instances are <strong>not</strong>
 * thread-safe.</p>
 */
final class Representations {

  private Representations() {
  }

  private static final DecimalFormatSymbols ENGLISH_SYMBOLS =
      DecimalFormatSymbols.getInstance(Locale.ENGLISH);

  static {
    ENGLISH_SYMBOLS.setNaN("NaN");
    ENGLISH_SYMBOLS.setInfinity("INF");
    ENGLISH_SYMBOLS.setDecimalSeparator('.');
    ENGLISH_SYMBOLS.setExponentSeparator("E");
    ENGLISH_SYMBOLS.setMinusSign('-');
  }

  // =========== RAW representations

  private static final Representation TO_STRING = new Representation() {
    @Nullable
    @Override
    public String apply(@Nullable final Object input) {
      return Objects.toString(input); // null-safe !
    }
  };

  /**
   * Defaults to {@link Object#toString()}
   */
  public static Representation javaString() {
    return TO_STRING;
  }

  private static final Representation DATE_VALUE_INSTANCE = new Representation() {
    @Override
    public String apply(final Object input) {
      return Long.toString(((java.util.Date) input).getTime());
    }
  };

  /**
   * {@link java.util.Date} ticks since epoch via {@link Long#toString(long)}.
   */
  public static Representation dateTicks() {
    return DATE_VALUE_INSTANCE;
  }

  // =========== PLAIN representations

  private static final Void VOID_INSTANCE = new Void();

  public static final String NULL_VALUE = "null";
  /**
   * textual representation of {@code null}, the magic constant {@link #NULL_VALUE}
   */
  public static Representation plainNull() {
    return VOID_INSTANCE;
  }

  private static final QuotedString QUOTED_INSTANCE = new QuotedString();

  /**
   * text enclosed in {@code "}(double quotes), with escaping of enclosed quotes
   */
  public static Representation quotedString() {
    return QUOTED_INSTANCE;
  }

  private static final Base64 BASE_64_INSTANCE = new Base64();

  /**
   * base64 encoding
   */
  public static Representation plainBinary() {
    return BASE_64_INSTANCE;
  }

  // =========== XML Schema representations

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#long">xs:long</a>
   */
  public static Representation xsLong() {
    final DecimalFormat format = new DecimalFormat("0", ENGLISH_SYMBOLS);
    format.setMaximumIntegerDigits(Integer.MAX_VALUE);
    return new FormatDelegate(format);
  }

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#decimal">xs:decimal</a>
   */
  public static Representation xsDecimal() {
    final DecimalFormat format = new DecimalFormat("0.0", ENGLISH_SYMBOLS);
    format.setMaximumIntegerDigits(Integer.MAX_VALUE);
    format.setMaximumFractionDigits(Integer.MAX_VALUE);
    return new FormatDelegate(format);
  }

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#double">xs:double</a>
   */
  public static Representation xsDouble() {
    final DecimalFormat format = new DecimalFormat("0.0E0", ENGLISH_SYMBOLS);
    format.setMaximumFractionDigits(Integer.MAX_VALUE);
    return new FormatDelegate(format);
  }

  private static final Boolean BOOLEAN_INSTANCE = new Boolean();

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#boolean">xs:boolean</a>
   */
  public static Representation xsBoolean() {
    return BOOLEAN_INSTANCE;
  }

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#dateTime">xs:dateTime</a>
   */
  public static Representation xsDateTime() {
    final SimpleDateFormat dateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);
    return new FormatDelegate(dateTimeFormat);
  }

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#time">xs:time</a>
   */
  public static Representation xsTime() {
    final SimpleDateFormat timeFormat =
        new SimpleDateFormat("HH:mm:ss.SSSXXX", Locale.ENGLISH);
    return new FormatDelegate(timeFormat);
  }

  /**
   * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.xml#date">xs:date</a>
   */
  public static Representation xsDate() {
    final SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    return new FormatDelegate(dateFormat);
  }

  /**
   * Create representation by delegating to a {@link java.text.Format}.
   */
  private static class FormatDelegate implements Representation {
    private final Format delegate;

    FormatDelegate(final Format delegate) {
      this.delegate = delegate;
    }

    @Override
    public final String apply(final Object input) {
      return delegate.format(input);
    }
  }


  /**
   * Convert {@link java.lang.Boolean} to {@code true} or {@code false}.
   */
  private static class Boolean implements Representation {
    public static final String TRUE_VALUE = "true";
    public static final String FALSE_VALUE = "false";

    private Boolean() {

    }

    @Override
    public String apply(final Object input) {
      return ((java.lang.Boolean) input) ? TRUE_VALUE : FALSE_VALUE;
    }
  }


  private static class QuotedString implements Representation {
    private static final Pattern QUOTE = Pattern.compile("\"");

    @Override
    public String apply(final Object input) {
      final CharSequence text = (CharSequence) input;
      // Matcher API requires StringBuffer
      final StringBuffer quoted = new StringBuffer(text.length() + 2); // optimistic size allocation
      final Matcher m = QUOTE.matcher(text);
      quoted.append('"');
      while(m.find()) {
        m.appendReplacement(quoted, "\"\"");
      }
      m.appendTail(quoted);
      quoted.append('"');
      return quoted.toString();
    }
  }


  private static class Void implements Representation {
    @Override
    public String apply(final Object input) {
      return NULL_VALUE;
    }
  }

  private static class Base64 implements Representation {
    private static final BaseEncoding BASE_64 = BaseEncoding.base64();
    @Override
    public String apply(final Object input) {
      return BASE_64.encode((byte[]) input);
    }
  }
}
