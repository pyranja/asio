package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.hamcrest.Matchers.*;
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
}
