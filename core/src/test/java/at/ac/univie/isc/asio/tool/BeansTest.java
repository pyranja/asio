package at.ac.univie.isc.asio.tool;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Properties;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class BeansTest {

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
