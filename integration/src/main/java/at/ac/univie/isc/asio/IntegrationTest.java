package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.restassured.ReportingFilter;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.EventSource;
import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.MatcherConfig;
import com.jayway.restassured.specification.RequestSpecification;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.assumeThat;

/**
 * Base for integration tests. Prepares RestAssured config and global rules. Tests can be adapted to
 * deployment specific configuration by setting {@link at.ac.univie.isc.asio.IntegrationTest#asio}
 * and {@link at.ac.univie.isc.asio.IntegrationTest#database}.
 */
@Category(Integration.class)
@RunWith(HierarchicalContextRunner.class)
public abstract class IntegrationTest {

  @BeforeClass
  public static void restAssuredDefaults() {
    RestAssured.config = RestAssured.config()
        .encoderConfig(EncoderConfig.encoderConfig()
                .defaultContentCharset(Charsets.UTF_8.name()).and()
                .defaultQueryParameterCharset(Charsets.UTF_8.name()).and()
                .appendDefaultContentCharsetToContentTypeIfUndefined(false)
        )
        .matcherConfig(
            new MatcherConfig().errorDescriptionType(MatcherConfig.ErrorDescriptionType.HAMCREST)
        )
    ;
  }

  /**
   * service address and authorization mode
   */
  public static AsioSpec asio = AsioSpec.withoutAuthorization(URI.create("http://localhost:8080/"));
  /**
   * integration database if available - may be null
   */
  public static Database database = null;

  @Rule
  public Timeout timeout = Rules.timeout(10, TimeUnit.SECONDS);
  @Rule
  public Interactions interactions = Rules.interactions();

  /**
   * @return the backing test database, if available
   * @throws AssertionError if no database is available
   */
  protected final Database database() {
    if (database == null) {
      throw new AssertionError("missing database configuration");
    }
    return database;
  }

  /**
   * @param permission required access rights
   * @return the service address
   */
  protected final URI serviceAddress(final String permission) {
    return asio.authorizedAddress(permission);
  }

  /**
   * Configure rest-assured to send authorized requests to the set service addressed.
   *
   * @param permission required access permission
   * @return rest-assured RequestSpecification for this test
   */
  protected final RequestSpecification givenPermission(final String permission) {
    final ReportingFilter reportingInterceptor = interactions.attached(ReportingFilter.create());
    return RestAssured.given()
        .spec(asio.requestWith(permission))
        .filter(reportingInterceptor);
  }

  /**
   * Alias for {@link #givenPermission(String permission)}.
   *
   * @param permission required access permission
   * @return rest-assured RequestSpecification for this test
   */
  protected final RequestSpecification withPermission(final String permission) {
    return givenPermission(permission);
  }

  /**
   * Create an {@link at.ac.univie.isc.asio.web.EventSource}, listening to the tested asio instance.
   *
   * @return configured event source
   */
  protected final EventSource eventSource() {
    return asio.eventSource();
  }

  /**
   * Test cases should use this HTTP header name to transmit delegated basic auth credentials
   *
   * @return name of the delegated credentials header
   */
  protected final String delegateCredentialsHeader() {
    return asio.getDelegateHeader();
  }

  /**
   * Skip test if no endpoint for the given language exists.
   *
   * @param language name of required language
   */
  protected final void ensureLanguageSupported(final String language) {
    final int responseCode =
        withPermission("admin").get("/{language}", language).then().extract().statusCode();
    assumeThat(language + " not supported", responseCode, is(not(HttpStatus.SC_NOT_FOUND)));
  }

  /**
   * Skip test if the service is not enforcing authorization, e.g. uses a fixed permission.
   */
  protected final void ensureSecured() {
    assumeThat("service is not secured", asio.isAuthorizing(), is(true));
  }

  /**
   * Skip test if the backing database is not configured, e.g. there is none.
   */
  protected final void ensureDatabaseAccessible() {
    assumeThat("database not accessible", database, is(not(nullValue())));
  }
}
