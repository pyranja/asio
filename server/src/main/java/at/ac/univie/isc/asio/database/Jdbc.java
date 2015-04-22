package at.ac.univie.isc.asio.database;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Jdbc connection settings.
 */
public final class Jdbc {
  /**
   * The name of the schema in the backing database, in MySQL the database name.
   * May be null if the connection grants global access.
   */
  @Pattern(regexp = "^[\\w-]+$")
  private String schema;
  /**
   * The JDBC connection url, e.g. 'jdbc:mysql://localhost/test'.
   */
  @NotNull
  private String url;
  /**
   * The class name of the JDBC driver. Inferred from the connection url if missing.
   */
  private String driver;
  /**
   * The login username. (default: '')
   */
  @NotNull
  private String username = "";
  /**
   * The login password. (default: '')
   */
  @NotNull
  private String password = "";
  /**
   * Additional or proprietary driver properties.
   */
  @NotNull
  private Map<String, String> properties = new HashMap<>();
  /**
   * Privileges that are granted to the user. If multi-tenancy is enabled, these are propagated to
   * the segragated users.
   */
  private List<String> privileges = new ArrayList<>();

  public String getSchema() {
    return schema;
  }

  public Jdbc setSchema(final String schema) {
    this.schema = schema;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public Jdbc setUrl(final String url) {
    this.url = url;
    return this;
  }

  public String getDriver() {
    return driver;
  }

  public Jdbc setDriver(final String driver) {
    this.driver = driver;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public Jdbc setUsername(final String username) {
    this.username = username;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public Jdbc setPassword(final String password) {
    this.password = password;
    return this;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public Jdbc setProperties(final Map<String, String> properties) {
    this.properties = properties;
    return this;
  }

  public Jdbc addProperty(final String key, final String value) {
    requireNonNull(key, "illegal property key");
    requireNonNull(value, "illegal property value");
    this.properties.put(key, value);
    return this;
  }

  public List<String> getPrivileges() {
    return privileges;
  }

  public Jdbc setPrivileges(final List<String> privileges) {
    this.privileges = privileges;
    return this;
  }

  public Jdbc addPrivilege(final String privilege) {
    this.privileges.add(privilege);
    return this;
  }

  @Override
  public String toString() {
    return "Jdbc{" +
        "schema='" + schema + '\'' +
        ", url='" + url + '\'' +
        ", driver='" + driver + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        ", properties=" + properties +
        ", privileges=" + privileges +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final Jdbc jdbc = (Jdbc) o;
    return Objects.equals(schema, jdbc.schema) &&
        Objects.equals(url, jdbc.url) &&
        Objects.equals(driver, jdbc.driver) &&
        Objects.equals(username, jdbc.username) &&
        Objects.equals(password, jdbc.password) &&
        Objects.equals(properties, jdbc.properties) &&
        Objects.equals(privileges, jdbc.privileges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, url, driver, username, password, properties, privileges);
  }
}
