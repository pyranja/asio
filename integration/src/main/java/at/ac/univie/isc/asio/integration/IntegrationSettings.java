package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.Uris;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.http.auth.UsernamePasswordCredentials;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Hold global integration test configuration.
 */
public final class IntegrationSettings {
  final IntegrationDsl dslDefaults;

  URI serviceBase;
  URI eventService = URI.create("events/");
  URI managementService = URI.create("api/");
  UsernamePasswordCredentials rootCredentials = new UsernamePasswordCredentials("anonymous", "");
  AuthMechanism auth = null;
  Database database = null;
  int timeoutInSeconds = 10;

  IntegrationSettings() {
    dslDefaults = new IntegrationDsl(new IntegrationDsl.SpecFactoryCallback() {
      @Override
      public RequestSpecification requestFrom(final IntegrationDsl args) {
        throw new AssertionError("request creation outside of test scope");
      }
    });
  }

  /**
   * Absolute base address of test subject.
   */
  @Nonnull
  public IntegrationSettings baseService(final URI serviceBase) {
    this.serviceBase = Uris.ensureDirectoryPath(serviceBase);
    return this;
  }

  /**
   * Address of event endpoint. May be relative to service base.
   */
  @Nonnull
  public IntegrationSettings eventService(final URI eventService) {
    this.eventService = Uris.ensureDirectoryPath(eventService);
    return this;
  }

  /**
   * Address of management endpoint. May be relative to service base.
   */
  @Nonnull
  public IntegrationSettings managementService(final URI managementService) {
    this.managementService = Uris.ensureDirectoryPath(managementService);
    return this;
  }

  /**
   * The login credentials for the management api.
   */
  @Nonnull
  public IntegrationSettings rootCredentials(final String username, final String password) {
    this.rootCredentials = new UsernamePasswordCredentials(username, password);
    return this;
  }

  /**
   * Authorization mechanism.
   */
  @Nonnull
  public IntegrationSettings auth(final AuthMechanism auth) {
    this.auth = auth;
    return this;
  }

  /**
   * Integration database if available. May be null.
   */
  @Nonnull
  public IntegrationSettings database(@Nullable final Database database) {
    this.database = database;
    return this;
  }

  /**
   * Global timeout for test cases.
   */
  @Nonnull
  public IntegrationSettings timeoutInSeconds(final int timeoutInSeconds) {
    this.timeoutInSeconds = timeoutInSeconds;
    return this;
  }

  /**
   * Modify request dsl defaults.
   */
  @Nonnull
  public IntegrationDsl defaults() {
    return dslDefaults;
  }

  void validate() {
    requireNonNull(serviceBase, "base service address missing");
    requireNonNull(eventService, "event service address missing");
    requireNonNull(auth, "no auth mechanism configured");
    requireNonNull(dslDefaults, "missing dsl defaults");
    assert timeoutInSeconds > 0 : "illegal timeout setting";
  }
}
