package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.d2rq.D2rqSpec;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.Resource;

import javax.validation.constraints.NotNull;

/**
 * nest configuration settings. getter/setter required by spring boot.
 */
@ConfigurationProperties("nest")
final class PhysicalSchemaSettings {
  /**
   * Local name of this schema, must be equal to the backing database schema.
   */
  @NotEmpty
  public String name;

  /**
   * Global identifier of this schema, must be equal to the key in the used metadata repository.
   */
  @NotEmpty
  public String identifier;

  /**
   * request timeout in milliseconds, negative values disable timeouts (default : 5000ms)
   */
  public long timeout = 5_000;

  @NestedConfigurationProperty
  @NotNull
  public Sparql sparql;

  @NestedConfigurationProperty
  @NotNull
  public Datasource datasource;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
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
    public Resource d2rMapping;

    /**
     * base uri used to resolve relative IRIS (default : asio:///default/)
     */
    @NotNull
    public String d2rBaseUri = D2rqSpec.DEFAULT_BASE;

    /**
     * whether federated sparql queries are allowed (default : false)
     */
    public boolean federation = false;

    public Resource getD2rMapping() {
      return d2rMapping;
    }

    public String getD2rBaseUri() {
      return d2rBaseUri;
    }

    public boolean isFederation() {
      return federation;
    }

    public void setD2rMapping(final Resource d2rMapping) {
      this.d2rMapping = d2rMapping;
    }

    public void setD2rBaseUri(final String d2rBaseUri) {
      this.d2rBaseUri = d2rBaseUri;
    }

    public void setFederation(final boolean federation) {
      this.federation = federation;
    }

    @Override
    public String toString() {
      return "Sparql{" +
          "d2rMapping=" + d2rMapping +
          ", d2rBaseUri='" + d2rBaseUri + '\'' +
          ", federation=" + federation +
          '}';
    }
  }


  public static final class Datasource {

    /**
     * the jdbc connection url
     */
    @NotEmpty
    public String jdbcUrl;

    /**
     * the jdbc username
     */
    @NotNull
    public String username;

    /**
     * the jdbc password
     */
    @NotNull
    public String password;

    public String getJdbcUrl() {
      return jdbcUrl;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
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
          "jdbcUrl='" + jdbcUrl + '\'' +
          ", username='" + username + '\'' +
          ", password='" + password + '\'' +
          '}';
    }
  }

  @Override
  public String toString() {
    return "NestSettings{" +
        "timeout=" + timeout +
        ", sparql=" + sparql +
        ", datasource=" + datasource +
        '}';
  }
}
