package at.ac.univie.isc.asio.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.rowset.WebRowSet;

import uk.org.ogsadai.converters.webrowset.WebRowSetResultSetParseException;
import uk.org.ogsadai.converters.webrowset.resultset.WebRowSetToResultSet;
import at.ac.univie.isc.asio.sql.KeyedRow;
import at.ac.univie.isc.asio.sql.KeyedRowConverter;
import at.ac.univie.isc.asio.sql.ResultSetIterator;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Convert a XML Webrowset into a map of key->[columns] pairs
 * 
 * @author Chris Borckholder
 */
public final class ResultSetToMap {

  private static final ResultSetToMap INSTANCE = new ResultSetToMap();

  /**
   * Consume the given stream as a {@link WebRowSet} and convert it.
   * 
   * @param stream a webrowset as binary data
   * @param keyColumn name of primary key column
   * @return map of {@link KeyedRow}s
   */
  public static Map<String, KeyedRow> convertStream(final InputStream stream, final String keyColumn) {
    try (InputStreamReader xml = new InputStreamReader(stream, Charsets.UTF_8)) {
      final WebRowSetToResultSet parser = new WebRowSetToResultSet(xml);
      // force eager parsing
      parser.setResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
      try (ResultSet rs = parser.getResultSet();) {
        return INSTANCE.convert(rs, keyColumn);
      }
    } catch (final SQLException | IOException | WebRowSetResultSetParseException e) {
      throw new IllegalStateException("failed to convert webrowset stream", e);
    }
  }

  public Map<String, KeyedRow> convert(final ResultSet rs, final String keyColumn) {
    final Builder<String, KeyedRow> partial = ImmutableMap.builder();
    final Iterable<KeyedRow> rows =
        ResultSetIterator.asIterable(rs, new KeyedRowConverter(keyColumn));
    for (final KeyedRow each : rows) {
      partial.put(each.getKey(), each);
    }
    return partial.build();
  }
}
