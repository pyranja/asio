package at.ac.univie.isc.asio.acceptance;

import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.tool.ClasspathResource;
import com.google.common.base.Throwables;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Hold acceptance test parameters. Static to enable working with JUnit test cases.
 */
public final class TestContext {
  static { // load jdbc driver
    try {
      Class.forName("org.h2.Driver");
    } catch (final ClassNotFoundException e) {
      System.err.println("failed to load H2 Driver - H2 .jar missing?");
    }
  }

  public static URI serverAddress() {
    final String address = System.getProperty(PROP_SERVER_ADDRESS, "https://localhost:8443/asio/");
    return URI.create(address);
  }

  public static KeyStore keyStore() {
    final String path = System.getProperty(PROP_KEYSTORE_PATH, "ssl/keystore");
    final ClasspathResource keystoreFile = ClasspathResource.fromRoot(path);
    try (final InputStream keyStream = keystoreFile.get().openStream()) {
      final KeyStore keyStore = KeyStore.getInstance("JKS");
      final String password = System.getProperty(PROP_KEYSTORE_PASSWORD, "asio-jetty");
      keyStore.load(keyStream, password.toCharArray());
      return keyStore;
    } catch (GeneralSecurityException | IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static Database database() {
    final String jdbcUrl = System.getProperty(PROP_JDBC_URL, "jdbc:h2:tcp://localhost/mem:test");
    final String user = System.getProperty(PROP_JDBC_USER, "root");
    final String password = System.getProperty(PROP_JDBC_PASSWORD, "change");
    return Database.create(jdbcUrl).credentials(user, password).build();
  }

  public static int timeout() {
    return Integer.valueOf(System.getProperty(PROP_TIMEOUT, "5"));
  }

  // property names
  public static final String PROP_SERVER_ADDRESS = "asio.test.server_uri";
  public static final String PROP_TIMEOUT = "asio.test.timeout";

  public static final String PROP_KEYSTORE_PATH = "asio.test.keystore_path";
  public static final String PROP_KEYSTORE_PASSWORD = "asio.test.keystore_password";

  public static final String PROP_JDBC_URL = "asio.test.jdbc_url";
  public static final String PROP_JDBC_USER = "asio.test.jdbc_user";
  public static final String PROP_JDBC_PASSWORD = "asio.test.jdbc_password";
}
