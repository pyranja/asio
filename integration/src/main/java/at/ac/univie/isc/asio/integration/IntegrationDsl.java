/*
 * #%L
 * asio integration
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.web.WebTools;
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
  private boolean manage = false;
  private UsernamePasswordCredentials delegated = null;

  /**
   * Initialize with request producing callback.
   */
  IntegrationDsl(final SpecFactoryCallback callback) {
    this.create = callback;
  }

  IntegrationDsl copy(final IntegrationDsl other) {
    this.schema = other.schema;
    this.manage = other.manage;
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
   * Access the management service, instead of a database schema.
   */
  public IntegrationDsl manage() {
    this.manage = true;
    return noSchema();
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
    return schema != null;
  }

  URI getSchemaPath() {
    return WebTools.ensureDirectoryPath(URI.create(schema));
  }

  boolean isManage() {
    return manage;
  }

  String getRole() {
    return role;
  }

  UsernamePasswordCredentials getDelegated() {
    return delegated;
  }
}
