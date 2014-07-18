package at.ac.univie.isc.asio.sql;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;

/**
 * Convert CSV data into an inmemory table representation.
 *
 * @author Chris Borckholder
 */
public class CsvToTable {

  public static final CsvToTable INSTANCE = new CsvToTable();

  public static Table<Integer, String, String> fromStream(final InputStream stream)
      throws IOException {
    final List<String[]> rows = rowsFrom(stream);
    final String[] header = rows.remove(0);
    return INSTANCE.convert(rows, header);
  }

  public static List<String> headerFrom(final InputStream stream) throws IOException {
    final List<String[]> rows = rowsFrom(stream);
    assert rows.size() > 0 : "no header found";
    return Arrays.asList(rows.get(0));
  }

  public static Table<Integer, String, String> fromArray(final byte[] data) throws IOException {
    return fromStream(new ByteArrayInputStream(data));
  }

  public static List<String> headerFrom(final byte[] data) throws IOException {
    return headerFrom(new ByteArrayInputStream(data));
  }

  private static List<String[]> rowsFrom(final InputStream stream) throws IOException {
    try (InputStreamReader csv = new InputStreamReader(stream, Charsets.UTF_8)) {
      final CSVReader reader = new CSVReader(csv);
      return reader.readAll();
    }
  }

  public static Table<Integer, String, String> fromChannel(final ReadableByteChannel channel)
      throws IOException {
    return fromStream(Channels.newInputStream(channel));
  }

  public Table<Integer, String, String> convert(final List<String[]> rows, final String[] header) {
    final Builder<Integer, String, String> partial = ImmutableTable.builder();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      final String[] row = rows.get(rowIndex);
      assert row.length == header.length : "row length mismatch with header @"
          + Arrays.toString(row);
      for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
        partial.put(rowIndex, header[columnIndex], row[columnIndex]);
      }
    }
    return partial.build();
  }
}
