package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.tool.TimeoutSpec;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Objects;

/**
 * Common dataset properties. These are not specific to an engine.
 */
final class Dataset {
  /**
   * Local name of the dataset.
   */
  @NotNull
  private Schema name;
  /**
   * Global identifier of the dataset.
   */
  @NotNull
  private URI identifier;
  /**
   * Maximal allowed duration of operations on this dataset. (default: undefined)
   */
  @NotNull
  private TimeoutSpec timeout = TimeoutSpec.undefined();
  /**
   * Whether federated query processing is supported. (default: false)
   */
  private boolean federationEnabled = false;

  public Schema getName() {
    return name;
  }

  public Dataset setName(final Schema name) {
    this.name = name;
    return this;
  }

  public URI getIdentifier() {
    return identifier;
  }

  public Dataset setIdentifier(final URI identifier) {
    this.identifier = identifier;
    return this;
  }

  public TimeoutSpec getTimeout() {
    return timeout;
  }

  public Dataset setTimeout(final TimeoutSpec timeout) {
    this.timeout = timeout;
    return this;
  }

  public boolean isFederationEnabled() {
    return federationEnabled;
  }

  public Dataset setFederationEnabled(final boolean federationEnabled) {
    this.federationEnabled = federationEnabled;
    return this;
  }

  @Override
  public String toString() {
    return "Dataset{" +
        "name=" + name +
        ", identifier='" + identifier + '\'' +
        ", timeout=" + timeout +
        ", federationEnabled=" + federationEnabled +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    final Dataset dataset = (Dataset) o;
    return Objects.equals(federationEnabled, dataset.federationEnabled) &&
        Objects.equals(name, dataset.name) &&
        Objects.equals(identifier, dataset.identifier) &&
        Objects.equals(timeout, dataset.timeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, identifier, timeout, federationEnabled);
  }
}
