package at.ac.univie.isc.asio.sql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import javax.sql.rowset.WebRowSet;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Convert a XML Webrowset into a map of key->[columns] pairs
 * 
 * @author Chris Borckholder
 */
public final class ResultSetToMap {

  private static final ResultSetToMap INSTANCE = new ResultSetToMap();

  /**
   * Consume the given stream as a {@link javax.sql.rowset.WebRowSet} and convert it.
   * 
   * @param stream a webrowset as binary data
   * @param keyColumn name of primary key column
   * @return map of {@link at.ac.univie.isc.asio.sql.KeyedRow}s
   */
  public static Map<String, KeyedRow> convertStream(final InputStream stream, final String keyColumn) {
    try (final InputStream ignored = stream) {
      final RowSetFactory rowSetFactory = RowSetProvider.newFactory();
      final WebRowSet webRowSet = rowSetFactory.createWebRowSet();
      webRowSet.readXml(stream);
      return INSTANCE.convert(webRowSet, keyColumn);
    } catch (final SQLException | IOException e) {
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
