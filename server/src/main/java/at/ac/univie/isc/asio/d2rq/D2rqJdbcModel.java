package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.Beans;
import at.ac.univie.isc.asio.tool.JdbcTools;
import org.d2rq.lang.D2RQMappingVisitor;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;
import org.d2rq.vocab.D2RQ;

import java.util.Map;

/**
 * Extract jdbc database from a d2rq mapping. Expect a single jdbc configuration block.
 */
public final class D2rqJdbcModel extends D2RQMappingVisitor.Default {
  /**
   * Create new mapping visitor, that finds jdbc database.
   *
   * @return initialized visitor
   */
  public static D2rqJdbcModel parse(final Mapping mapping) {
    final D2rqJdbcModel finder = new D2rqJdbcModel();
    mapping.accept(finder);
    if (!finder.visited) { throw new InvalidD2rqConfig(D2RQ.Database, "missing"); }
    return finder;
  }

  private boolean visited = false;

  private String schema;
  private String url;
  private String driver;
  private String username;
  private String password;
  private Map<String, String> properties;

  D2rqJdbcModel() { /* --- */ }

  @Override
  public boolean visitEnter(final Mapping mapping) {
    assert !visited : "JdbcFinder reuse is illegal";
    return true;
  }

  @Override
  public void visit(final Database database) {
    if (visited) { throw new InvalidD2rqConfig(D2RQ.Database, "found more than one"); }
    visited = true;
    url = database.getJdbcURL();
    driver = database.getJDBCDriver();
    username = nullToEmpty(database.getUsername());
    password = nullToEmpty(database.getPassword());
    schema = database.getConnectionProperties().getProperty("schema");
    properties = Beans.copyToMap(database.getConnectionProperties(), "schema");
    if (!JdbcTools.isValidJdbcUrl(url)) {
      throw new InvalidD2rqConfig(D2RQ.jdbcURL, "<" + url + "> is not valid");
    }
  }

  public String getSchema() {
    return schema;
  }

  public String getUrl() {
    return url;
  }

  public String getDriver() {
    return driver;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  private String nullToEmpty(final String raw) {
    return raw == null ? "" : raw;
  }
}
