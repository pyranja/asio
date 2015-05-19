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
package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class PrettyTest {

  public static class Justify {
    @Test
    public void too_long_string_is_not_padded() throws Exception {
      final String result = Pretty.justify("test", 2, '#');
      assertThat(result, is("test"));
    }

    @Test
    public void pads_start_with_given_char() throws Exception {
      final String result = Pretty.justify("test", 10, '#');
      assertThat(result, startsWith("###"));
    }

    @Test
    public void pads_end_with_given_char() throws Exception {
      final String result = Pretty.justify("test", 10, '#');
      assertThat(result, endsWith("###"));
    }

    @Test
    public void no_padding_if_string_has_correct_length() throws Exception {
      final String result = Pretty.justify("test", 4, '#');
      assertThat(result, is("test"));
    }

    @Test
    public void pads_either_only_start_or_end_when_length_one_off() throws Exception {
      final String result = Pretty.justify("test", 5, '#');
      assertThat(result, either(is("#test")).or(is("test#")));
    }
  }

  public static class Compact {
    @Test
    public void should_leave_single_line_text_as_is() throws Exception {
      assertThat(Pretty.compact("my simple command"), is("my simple command"));
    }

    @Test
    public void should_replace_new_lines_with_whitespace() throws Exception {
      final String text = String.format(Locale.ENGLISH, "my three%nline%ncommand");
      assertThat(Pretty.compact(text), is("my three line command"));
    }

    @Test
    public void should_strip_trailing_whitespace() throws Exception {
      assertThat(Pretty.compact("my command    "), is("my command"));
    }

    @Test
    public void should_strip_leading_whitespace() throws Exception {
      assertThat(Pretty.compact("    my command"), is("my command"));
    }

    @Test
    public void should_collapse_internal_whitespace() throws Exception {
      assertThat(Pretty.compact("my  \t command"), is("my command"));
    }
  }

  public static class Substitute {
    private final Map<String, ?> values =
        ImmutableMap.of("key-one", "value-one", "key-two", "value-two");

    @Test
    public void noop_if_no_variables_present()throws Exception {
      final String result = Pretty.substitute("no variables", values);
      assertThat(result, is("no variables"));
    }

    @Test
    public void replace_single_variable() throws Exception {
      final String result = Pretty.substitute("${key-one}", values);
      assertThat(result, is("value-one"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_on_missing_value() throws Exception {
      Pretty.substitute("${missing}", values);
    }

    @Test
    public void replace_variables_by_key() throws Exception {
      final String result = Pretty.substitute("${key-two}|${key-one}|${key-two}", values);
      assertThat(result, is("value-two|value-one|value-two"));
    }

    @Test
    public void partial_tokens_not_substituted() throws Exception {
      final String result = Pretty.substitute("${key-one", values);
      assertThat(result, is("${key-one"));
    }

    @Test
    public void nested_token_in_replacement_are_treated_as_literals() throws Exception {
      final String result = Pretty.substitute("${outer}", ImmutableMap.of("outer", "${inner}"));
      assertThat(result, is("${inner}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_on_empty_token() throws Exception {
      Pretty.substitute("${}", ImmutableMap.of("", "empty"));
    }
  }

  public static class Expand {
    @Test
    public void local_only_qname() throws Exception {
      final QName qName = new QName("local-only");
      assertThat(Pretty.expand(qName), is("local-only"));
    }

    @Test
    public void qualified_name() throws Exception {
      final QName qname = new QName("http://test.com/", "local");
      assertThat(Pretty.expand(qname), is("http://test.com/local"));
    }

    @Test
    public void qualified_name_with_prefix() throws Exception {
      final QName qname = new QName("http://test.com/", "local", "prefix");
      assertThat(Pretty.expand(qname), is("http://test.com/local"));
    }
  }
}
