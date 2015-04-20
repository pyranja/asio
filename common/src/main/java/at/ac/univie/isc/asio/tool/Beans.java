package at.ac.univie.isc.asio.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Properties;

/**
 * Utility methods to work with beans and collections.
 */
public final class Beans {
  private Beans() { /* no instances */ }

  /**
   * Create a copy of given input properties.
   *
   * @param input source properties
   * @param exclude keys of properties that should be omitted
   * @return map of copied properties
   */
  public static Map<String, String> copyToMap(final Properties input, final String... exclude) {
    final ImmutableSet<String> excluded = ImmutableSet.copyOf(exclude);
    final ImmutableMap.Builder<String, String> clone = ImmutableMap.builder();
    for (String key : input.stringPropertyNames()) {
      if (!excluded.contains(key)) {
        clone.put(key, input.getProperty(key));
      }
    }
    return clone.build();
  }

  /**
   * Create a {@link Properties} instance holding all entries from the given map.
   *
   * @param map source map
   * @return properties with all entries of input map
   */
  public static Properties asProperties(final Map<String, String> map) {
    final Properties result = new Properties();
    result.putAll(map);
    return result;
  }
}
