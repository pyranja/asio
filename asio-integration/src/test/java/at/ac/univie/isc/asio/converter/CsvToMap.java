package at.ac.univie.isc.asio.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import at.ac.univie.isc.asio.sql.KeyedRow;
import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

/**
 * Convert a CSV resultset into a map of key->[columns] pairs
 * 
 * @author Chris Borckholder
 */
public final class CsvToMap {

  private static final CsvToMap INSTANCE = new CsvToMap();

  public static Map<String, KeyedRow> convertStream(final InputStream stream, final String keyColumn) {
    try (InputStreamReader csv = new InputStreamReader(stream, Charsets.UTF_8)) {
      final CSVReader reader = new CSVReader(csv);
      final List<String[]> rows = reader.readAll();
      final int keyIndex = determineKeyIndex(rows.get(0), keyColumn);
      rows.remove(0); // skip header
      return INSTANCE.convert(rows, keyIndex);
    } catch (final IOException e) {
      throw new IllegalStateException("failed to convert csv stream", e);
    }
  }

  private static int determineKeyIndex(final String[] header, final String keyColumn) {
    for (int i = 0; i < header.length; i++) {
      if (keyColumn.equalsIgnoreCase(header[i])) {
        return i;
      }
    }
    throw new IllegalStateException("failed to find key column (" + keyColumn + ") index in "
        + header);
  }

  /**
   * expects the header to be skipped.
   * 
   * @param rows csv iterator
   * @param keyIndex index of PK column
   * @return map of keyed rows
   */
  public Map<String, KeyedRow> convert(final List<String[]> rows, final int keyIndex) {
    final ImmutableMap.Builder<String, KeyedRow> partial = ImmutableMap.builder();
    for (final String[] each : rows) {
      final String key = each[keyIndex];
      final KeyedRow row = new KeyedRow(key, Arrays.asList(each));
      partial.put(key, row);
    }
    return partial.build();
  }
}
