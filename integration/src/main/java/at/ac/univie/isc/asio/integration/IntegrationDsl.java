package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.web.Uris;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.auth.UsernamePasswordCredentials;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Fluent builder for integration test requests.
 */
public final class IntegrationDsl {
  static interface SpecFactoryCallback {
    RequestSpecification requestFrom(IntegrationDsl args);
  }

  private final SpecFactoryCallback create;
  private String schema;
  private String role;
  private UsernamePasswordCredentials delegated = null;

  /**
   * Initialize with request producing callback.
   */
  IntegrationDsl(final SpecFactoryCallback callback) {
    this.create = callback;
  }

  IntegrationDsl copy(final IntegrationDsl other) {
    this.schema = other.schema;
    this.role = other.role;
    this.delegated = other.delegated;
    return this;
  }

  /**
   * Change the implicit target schema.
   */
  public IntegrationDsl schema(final String schema) {
    this.schema = schema;
    return this;
  }

  /**
   * Do not add a schema to the path implicitly.
   */
  public IntegrationDsl noSchema() {
    this.schema = null;
    return this;
  }

  /**
   * Change the role used for authorization.
   */
  public IntegrationDsl role(final String role) {
    this.role = requireNonNull(role);
    return this;
  }

  /**
   * Attach the given username/password pair as delegated credentials to the request.
   */
  public IntegrationDsl delegate(final String username, final String password) {
    this.delegated = new UsernamePasswordCredentials(username, password);
    return this;
  }

  /**
   * Alias for {@link #spec()}.
   */
  public RequestSpecification and() {
    return spec();
  }

  /**
   * Use settings to create a rest-assured request spec.
   */
  public RequestSpecification spec() {
    return create.requestFrom(this);
  }

  // === getter ====================================================================================

  boolean hasSchema() {
    return schema == null;
  }

  URI getSchemaPath() {
    return Uris.ensureDirectoryPath(URI.create(schema));
  }

  String getRole() {
    return role;
  }

  UsernamePasswordCredentials getDelegated() {
    return delegated;
  }
}
