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

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * reference canonical type representations
 */
@RunWith(Enclosed.class)
public abstract class RepresentationTest {
  public static final String PARAM_FORMAT = "{index}: format({0}) => \"{1}\"";

  @Parameterized.Parameter(0)
  public Object input;
  @Parameterized.Parameter(1)
  public String expected;

  protected abstract Representation subject();

  @Test
  public final void format_to_expected() {
    final String formatted = subject().apply(input);
    final String reason =
        String.format("wrong representation of %s (%s)", input, input.getClass().getSimpleName());
    assertThat(reason, formatted, is(expected));
  }

  public static class Defaults {
    @Test
    public void to_string_fallback_is_null_safe() throws Exception {
      Representations.javaString().apply(null);
      // don't care for actual return value
    }

    @Test
    public void null_presenter_yields_magic_constant() throws Exception {
      final String formatted = Representations.plainNull().apply(new Object());
      assertThat(formatted, is(sameInstance(Representations.NULL_VALUE)));
    }
  }

  @RunWith(Parameterized.class)
  public static class XsdLong extends RepresentationTest {
    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {0L, "0"}, {-0L, "0"}
              , {1L, "1"}, {-1L, "-1"}
              , {Long.MAX_VALUE, "9223372036854775807"}
              , {Long.MIN_VALUE, "-9223372036854775808"}
              , {Integer.MAX_VALUE, "2147483647"}
              , {Integer.MIN_VALUE, "-2147483648"}
          });
    }

    @Override
    protected Representation subject() {
      return Representations.xsLong();
    }
  }


  @RunWith(Parameterized.class)
  public static class XsdDecimal extends RepresentationTest {
    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {BigDecimal.ZERO, "0.0"}, {BigDecimal.ZERO.negate(), "0.0"}
              , {BigDecimal.ONE, "1.0"}, {BigDecimal.ONE.negate(), "-1.0"}
              , {BigDecimal.valueOf(0.1d), "0.1"}, {new BigDecimal(".1"), "0.1"}
              , {BigDecimal.valueOf(Long.MAX_VALUE), "9223372036854775807.0"}
              , {BigDecimal.valueOf(Long.MIN_VALUE), "-9223372036854775808.0"}
              , {new BigDecimal("+9223372036854775807.9223372036854775807"), "9223372036854775807.9223372036854775807"}
              , {new BigDecimal("000003405.231340000"), "3405.23134"}
              , {BigDecimal.valueOf(Integer.MAX_VALUE), "2147483647.0"}
              , {BigDecimal.valueOf(Integer.MIN_VALUE), "-2147483648.0"}
              // from http://stackoverflow.com/questions/5749615/losing-precision-converting-from-java-bigdecimal-to-double
              , {new BigDecimal("299792.4579999984"), "299792.4579999984"}
              , {new BigDecimal("299792.45799999984"), "299792.45799999984"}
              , {new BigDecimal("299792.457999999984"), "299792.457999999984"}
          });
    }

    @Override
    protected Representation subject() {
      return Representations.xsDecimal();
    }
  }


  @RunWith(Parameterized.class)
  public static class XsdDouble extends RepresentationTest {
    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {0.0d, "0.0E0"}, {-0.0d, "-0.0E0"}
              , {1.0d, "1.0E0"}, {-1.0d, "-1.0E0"}
              , {0.1d, "1.0E-1"}, {-0.1d, "-1.0E-1"}
              , {000001234.567890000d, "1.23456789E3"}
              , {Double.MAX_VALUE, "1.7976931348623157E308"}
              , {Double.MIN_VALUE, "4.9E-324"}
              , {BigDecimal.valueOf(Integer.MAX_VALUE).doubleValue(), "2.147483647E9"}
              , {BigDecimal.valueOf(Integer.MIN_VALUE).doubleValue(), "-2.147483648E9"}
              , {123456.12345678912d, "1.2345612345678912E5"}
              , {123456.123456789123d, "1.2345612345678912E5"}  // precision limit
              , {0.12345678912345678d, "1.2345678912345678E-1"}
              , {0.1234567891234567899999d, "1.2345678912345678E-1"}  // precision limit
              // from http://stackoverflow.com/questions/5749615/losing-precision-converting-from-java-bigdecimal-to-double
              , {299792.4579999984d, "2.997924579999984E5"}
              , {299792.45799999984d, "2.9979245799999987E5"}
              , {299792.457999999984d, "2.99792458E5"}
              , {Double.NaN, "NaN"}
              , {Double.POSITIVE_INFINITY, "INF"}
              , {Double.NEGATIVE_INFINITY, "-INF"}
          });
    }

    @Override
    protected Representation subject() {
      return Representations.xsDouble();
    }
  }


  @RunWith(Parameterized.class)
  public static class XsdBoolean extends RepresentationTest {
    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {Boolean.TRUE, "true"}
              , {Boolean.FALSE, "false"}
          });
    }

    @Override
    protected Representation subject() {
      return Representations.xsBoolean();
    }
  }

  @RunWith(Parameterized.class)
  public static class XsDatetime extends RepresentationTest {
    @Override
    protected Representation subject() {
      return Representations.xsDateTime();
    }

    private static final Calendar NOW = Calendar.getInstance(Locale.ENGLISH);

    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {new Timestamp(NOW.getTimeInMillis()), DatatypeConverter.printDateTime(NOW)}
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class XsTime extends RepresentationTest {
    @Override
    protected Representation subject() {
      return Representations.xsTime();
    }

    private static final Calendar NOW = Calendar.getInstance(Locale.ENGLISH);

    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {new Time(NOW.getTimeInMillis()), DatatypeConverter.printTime(NOW)}
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class XsDate extends RepresentationTest {
    @Override
    protected Representation subject() {
      return Representations.xsDate();
    }

    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {new java.sql.Date(0L), "1970-01-01"}
              , {new java.sql.Date(70, 0, 1), "1970-01-01"}
              , {new java.sql.Date(114, 5, 20), "2014-06-20"}
              , {new java.util.Date(114, 5, 20), "2014-06-20"}
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class QuotedString extends RepresentationTest {
    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {"","\"\""}
              , {"test", "\"test\""}
              , {"quo\"ted", "\"quo\"\"ted\""}
              , {"double\"\"quote", "\"double\"\"\"\"quote\""}
          });
    }

    @Override
    protected Representation subject() {
      return Representations.quotedString();
    }
  }

  public static class PlainNull {
    @Test
    public void format_null() throws Exception {
      assertThat(Representations.plainNull().apply(null), is("null"));
    }

    @Test
    public void format_non_null() throws Exception {
      assertThat(Representations.plainNull().apply(new Object()), is("null"));
    }
  }

  @RunWith(Parameterized.class)
  public static class PlainBinary extends RepresentationTest {
    @Parameterized.Parameters(name = PARAM_FORMAT)
    public static Iterable<Object[]> data() {
      return Arrays.asList(new Object[][]
          {
              {new byte[0], ""}
              , {new byte[]{0}, "AA=="}
              , {new byte[]{0,0}, "AAA="}
              , {new byte[]{0,0,0}, "AAAA"}
          });
    }

    @Override
    protected Representation subject() {
      return Representations.plainBinary();
    }
  }
}
