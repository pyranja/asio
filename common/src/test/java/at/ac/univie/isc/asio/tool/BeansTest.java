package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class BeansTest {

  public static class AsProperties {
    @Test
    public void should_create_empty_properties_from_empty_map() throws Exception {
      final Properties result = Beans.asProperties(Collections.<String, String>emptyMap());
      assertThat(result.values(), empty());
    }

    @Test
    public void should_have_all_entries_from_input_map() throws Exception {
      final ImmutableMap<String, String> input =
          ImmutableMap.of("first", "val-one", "second", "val-two");
      final Map<Object, Object> result = Beans.asProperties(input);
      assertThat(result, Matchers.<Object, Object>hasEntry("first", "val-one"));
      assertThat(result, Matchers.<Object, Object>hasEntry("second", "val-two"));
    }
  }

  public static class CopyToMap {

    @Test
    public void should_yield_empty_map_on_empty_input() throws Exception {
      assertThat(Beans.copyToMap(new Properties()).values(), empty());
    }

    @Test
    public void should_copy_entries_of_input() throws Exception {
      final Properties input = new Properties();
      input.setProperty("key", "value");
      assertThat(Beans.copyToMap(input), hasEntry("key", "value"));
    }

    @Test
    public void should_ignore_non_string_keys() throws Exception {
      final Properties input = new Properties();
      input.put("key", new Object());
      assertThat(Beans.copyToMap(input).values(), empty());
    }

    @Test
    public void should_ignore_non_string_values() throws Exception {
      final Properties input = new Properties();
      input.put(new Object(), "value");
      assertThat(Beans.copyToMap(input).values(), empty());
    }

    @Test
    public void should_ignore_non_string_entries() throws Exception {
      final Properties input = new Properties();
      input.put(new Object(), new Object());
      assertThat(Beans.copyToMap(input).values(), empty());
    }

    @Test(expected = NullPointerException.class)
    public void should_fail_on_null_input() throws Exception {
      Beans.copyToMap(null);
    }

    @Test
    public void should_include_entries_from_defaults() throws Exception {
      final Properties defaults = new Properties();
      defaults.setProperty("default-key", "default-value");
      final Properties input = new Properties(defaults);
      assertThat(Beans.copyToMap(input), hasEntry("default-key", "default-value"));
    }

    @Test
    public void should_omit_entries_with_excluded_keys() throws Exception {
      final Properties input = new Properties();
      input.setProperty("ignore-me", "ignored");
      input.setProperty("include-me", "included");
      assertThat(Beans.copyToMap(input, "ignore-me").keySet(), not(hasItem("ignore-me")));
    }
  }
}
