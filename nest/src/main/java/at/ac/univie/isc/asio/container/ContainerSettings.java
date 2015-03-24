package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.d2rq.D2rqSpec;
import at.ac.univie.isc.asio.tool.BeanToMap;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;

/**
 * nest configuration settings. getter/setter required by spring boot.
 */
@ConfigurationProperties("nest")
public final class ContainerSettings {

  /**
   * @param schema local name of schema
   * @return settings with name set to the schema name
   */
  public static ContainerSettings of(final Schema schema) {
    final ContainerSettings it = new ContainerSettings();
    it.setName(schema);
    return it;
  }

  /**
   * Create a deep copy of given settings.
   *
   * @param source original settings
   * @return deep copy of settings
   */
  public static ContainerSettings copy(final ContainerSettings source) {
    final ContainerSettings cloned = new ContainerSettings();
    BeanUtils.copyProperties(source, cloned);
    if (source.getSparql() != null) {
      final Sparql sparql = new Sparql();
      BeanUtils.copyProperties(source.getSparql(), sparql);
      cloned.setSparql(sparql);
    }
    if (source.getDatasource() != null) {
      final Datasource datasource = new Datasource();
      BeanUtils.copyProperties(source.getDatasource(), datasource);
      cloned.setDatasource(datasource);
    }
    return cloned;
  }

  /**
   * Convert into a flattened map of properties.
   */
  public Map<String, Object> asMap() {
    return BeanToMap.withPrefix("nest").convert(this);
  }

  /**
   * Local name of this container.
   */
  @NotNull
  Schema name;

  /**
   * Global identifier of this schema, must be equal to the key in the used metadata repository.
   */
  @NotEmpty
  String identifier;

  /**
   * request timeout in milliseconds, negative values disable timeouts (default : 5000ms)
   */
  long timeout = 5_000;

  @NestedConfigurationProperty
  @NotNull
  Sparql sparql;

  @NestedConfigurationProperty
  @NotNull
  Datasource datasource;

  public ContainerSettings() {
  }

  public Schema getName() {
    return name;
  }

  public String getIdentifier() {
    return identifier;
  }

  public long getTimeout() {
    return timeout;
  }

  public Sparql getSparql() {
    return sparql;
  }

  public Datasource getDatasource() {
    return datasource;
  }

  public void setName(final Schema name) {
    this.name = name;
  }

  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
  }

  public void setTimeout(final long timeout) {
    this.timeout = timeout;
  }

  public void setSparql(final Sparql sparql) {
    this.sparql = sparql;
  }

  public void setDatasource(final Datasource datasource) {
    this.datasource = datasource;
  }

  public static final class Sparql {

    /**
     * location of d2r mapping file
     */
    @NotNull
    URI d2rMappingLocation;

    /**
     * base uri used to resolve relative IRIS (default : asio:///default/)
     */
    @NotNull
    URI d2rBaseUri = URI.create(D2rqSpec.DEFAULT_BASE);

    /**
     * whether federated sparql queries are allowed (default : false)
     */
    boolean federation = false;

    public URI getD2rMappingLocation() {
      return d2rMappingLocation;
    }

    public URI getD2rBaseUri() {
      return d2rBaseUri;
    }

    public boolean isFederation() {
      return federation;
    }

    public void setD2rMappingLocation(final URI d2rMappingLocation) {
      this.d2rMappingLocation = d2rMappingLocation;
    }

    public void setD2rBaseUri(final URI d2rBaseUri) {
      this.d2rBaseUri = d2rBaseUri;
    }

    public void setFederation(final boolean federation) {
      this.federation = federation;
    }

    @Override
    public String toString() {
      return "Sparql{" +
          "d2rMappingLocation=" + d2rMappingLocation +
          ", d2rBaseUri='" + d2rBaseUri + '\'' +
          ", federation=" + federation +
          '}';
    }
  }


  public static final class Datasource {

    /**
     * The name of the schema holding the data in the backing database. In case of MySQL this is
     * the database name. (defaults to the container name if missing)
     */
    String schema;

    /**
     * the jdbc connection url
     */
    @NotEmpty
    String jdbcUrl;

    /**
     * the jdbc username
     */
    @NotNull
    String username;

    /**
     * the jdbc password
     */
    @NotNull
    String password;

    public String getSchema() {
      return schema;
    }

    public String getJdbcUrl() {
      return jdbcUrl;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public void setSchema(final String schema) {
      this.schema = schema;
    }

    public void setJdbcUrl(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    public void setUsername(final String username) {
      this.username = username;
    }

    public void setPassword(final String password) {
      this.password = password;
    }

    @Override
    public String toString() {
      return "Datasource{" +
          "schema='" + schema + '\'' +
          ", jdbcUrl='" + jdbcUrl + '\'' +
          ", username='" + username + '\'' +
          ", password='" + password + '\'' +
          '}';
    }
  }

  @Override
  public String toString() {
    return "ContainerSettings{" +
        "name=" + name +
        ", identifier='" + identifier + '\'' +
        ", timeout=" + timeout +
        ", sparql=" + sparql +
        ", datasource=" + datasource +
        '}';
  }
}
