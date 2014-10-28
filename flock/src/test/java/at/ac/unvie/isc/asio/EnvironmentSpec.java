package at.ac.unvie.isc.asio;

import com.google.common.base.Objects;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Capture environment variables required by asio integration tests.
 * A static {@link #current holder} allows junit tests to access the spec.
 */
public final class EnvironmentSpec {
  /** the environment spec of the current execution context */
  public static EnvironmentSpec current = new EnvironmentSpec(URI.create("http://localhost:8080"));

  /**
   * Create a new spec and update the {@link #current holder}.
   * @param serverAddress base URI of tested asio instance
   * @return initial spec
   */
  static EnvironmentSpec create(final URI serverAddress) {
    current = new EnvironmentSpec(serverAddress);
    return current;
  }

  private final URI serverAddress;
  private URI sparqlEndpoint;

  private EnvironmentSpec(final URI serverAddress) {
    this.serverAddress = requireNonNull(serverAddress);
  }

  public URI serverAddress() {
    return failIfUnset(serverAddress, "server.address");
  }

  public URI sparqlEndpoint() {
    return failIfUnset(sparqlEndpoint, "sparql.endpoint");
  }

  private <T> T failIfUnset(final T reference, final String label) {
    if (reference == null) {
      throw new AssertionError(label + " not initialized. check environment spec?");
    }
    return reference;
  }

  // === modifier ==================================================================================

  EnvironmentSpec sparql(final URI path) {
    sparqlEndpoint = serverAddress().resolve(path);
    return this;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("serverAddress", serverAddress)
        .add("sparqlEndpoint", sparqlEndpoint)
        .toString();
  }
}
