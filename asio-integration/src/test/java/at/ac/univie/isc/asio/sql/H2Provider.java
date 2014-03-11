package at.ac.univie.isc.asio.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Create JDBC connections to the H2 integration test db.
 * 
 * @author Chris Borckholder
 */
// XXX consider using spring jdbc templates and configuration
public class H2Provider {

  // XXX these settings should come from configuration
  // -> use maven properties plugin
  private static final String H2_URL = "jdbc:h2:tcp://localhost/mem:test";
  private static final String USER = "root";
  private static final String PASSWORD = "change";

  private static final String H2_DRIVER_CLASS = "org.h2.Driver";

  static { // load H2 driver
    try {
      Class.forName(H2_DRIVER_CLASS);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException("failed to load H2 Driver - H2 .jar missing?");
    }
  }

  public static Connection connect() throws SQLException {
    return DriverManager.getConnection(H2_URL, USER, PASSWORD);
  }
}
