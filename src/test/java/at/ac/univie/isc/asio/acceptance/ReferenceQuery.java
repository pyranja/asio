package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.sql.H2Provider;
import at.ac.univie.isc.asio.sql.KeyedRow;
import at.ac.univie.isc.asio.sql.ResultSetToMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;

/**
 * Execute a query against the integration database and hold the result data as an in-memory map to
 * compare against other query results.
 * 
 * @author Chris Borckholder
 */
public class ReferenceQuery {

  /**
   * factory for reference data
   * 
   * @param query sql reference query
   * @param keyColumn primary key of data
   * @return reference query data
   */
  public static ReferenceQuery forQuery(final String query, final String keyColumn) {
    try (Connection connection = H2Provider.connect();) {
      return new ReferenceQuery(connection, query, keyColumn);
    } catch (final SQLException e) {
      throw new IllegalStateException("failed to initialize reference data", e);
    }
  }

  private final Map<String, KeyedRow> reference;
  private final String query;

  public ReferenceQuery(final Connection conn, final String query, final String keyColumn) {
    super();
    this.query = query;
    final ResultSetToMap converter = new ResultSetToMap();
    try (ResultSet rs = fetch(conn);) {
      reference = converter.convert(rs, keyColumn);
    } catch (final SQLException e) {
      throw new IllegalStateException("failed to initialize reference data", e);
    }
  }

  public Map<String, KeyedRow> getReference() {
    return Collections.unmodifiableMap(reference);
  }

  @Override
  public String toString() {
    return String.format("ReferenceQuery [query=%s]", query);
  }

  private ResultSet fetch(final Connection conn) throws SQLException {
    final Statement stmt = conn.createStatement();
    return stmt.executeQuery(query);
  }
}
