package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.sql.ResultSetIterator;
import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Transform serialized result data into an in-memory {@link com.google.common.collect.Table}.
 */
public class ConvertToTable {
  public static final String NULL = "null"; // signal a null value

  public static Table<Integer, String, String> fromCsv(final InputStream stream) throws IOException {
    try (final InputStreamReader csv = new InputStreamReader(stream, Charsets.UTF_8)) {
      final CSVReader reader = new CSVReader(csv);
      final List<String[]> rows = reader.readAll();
      assert rows.size() > 0 : "no header found";
      final String[] header = rows.remove(0);
      return INSTANCE.convert(rows, header);
    }
  }

  public static Table<Integer, String, String> fromResultSet(final ResultSet rs) throws SQLException {
    final ResultSetMetaData rsmd = rs.getMetaData();
    final String[] header = new String[rsmd.getColumnCount()];
    for (int index = 0; index < header.length; index++) {
      header[index] = rsmd.getColumnName(index + 1);
    }
    final Iterable<String[]> rows = ResultSetIterator.asIterable(rs, new RowToStringArray());
    return INSTANCE.convert(rows, header);
  }

  private static class RowToStringArray implements ResultSetIterator.RowConverter<String[]> {
    @Nonnull
    @Override
    public String[] process(@Nonnull final ResultSet resultSet) throws SQLException {
      final String[] row = new String[resultSet.getMetaData().getColumnCount()];
      for (int i = 0; i < row.length; i++) {
        row[i] = resultSet.getString(i+1);
      }
      return row;
    }
  }

  private static final ConvertToTable INSTANCE = new ConvertToTable();

  private ConvertToTable() {}

  public Table<Integer, String, String> convert(final Iterable<String[]> rows, final String[] header) {
    final ImmutableTable.Builder<Integer, String, String> table = ImmutableTable.builder();
    int rowIndex = 0;
    for (String[] row : rows) {
      assert row.length == header.length : "row length mismatch with header @" + Arrays.toString(row);
      for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
        final String value = normalize(row[columnIndex]);
        table.put(rowIndex, header[columnIndex], value);
      }
      rowIndex++;
    }
    return table.build();
  }

  private static String normalize(final String s) {
    final String value;
    if (s == null) {
      value = NULL;
    } else {
      value = s.toLowerCase(Locale.ENGLISH);
    }
    return value;
  }
}
