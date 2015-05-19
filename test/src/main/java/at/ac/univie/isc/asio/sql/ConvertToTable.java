/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.sql;

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
 * The table's row key is the row's zero-based index in the result set,
 * the column key is the name of the column in the result set and
 * the cell value is the string representation of the result values.
 */
public class ConvertToTable {
  /** magic value, which signals a true {@code null} value in the input */
  public static final String NULL = "null";

  /**
   * Convert the given stream of {@code comma separated values} to a table.
   * @param stream utf-8 encoded input
   * @return in-memory table representation
   * @throws IOException if reading the stream fails.
   */
  public static Table<Integer, String, String> fromCsv(final InputStream stream) throws IOException {
    try (final InputStreamReader csv = new InputStreamReader(stream, Charsets.UTF_8)) {
      final CSVReader reader = new CSVReader(csv);
      final List<String[]> rows = reader.readAll();
      assert rows.size() > 0 : "no header found";
      final String[] header = rows.remove(0);
      return INSTANCE.convert(rows, header);
    }
  }

  /**
   * Convert the given JDBC {@code ResultSet} to a table.
   * The result set is fully consumed, but <strong>not</strong> closed.
   * @param rs the result set
   * @return in-memory table representation
   * @throws SQLException if reading the result set fails
   */
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

  private Table<Integer, String, String> convert(final Iterable<String[]> rows, final String[] header) {
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
