package at.ac.univie.isc.asio.integration;

import at.ac.univie.isc.asio.Integration;
import at.ac.univie.isc.asio.Unchecked;
import at.ac.univie.isc.asio.atos.FakeAtosService;
import at.ac.univie.isc.asio.insight.EventReport;
import at.ac.univie.isc.asio.insight.EventStream;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.junit.Interactions;
import at.ac.univie.isc.asio.junit.Rules;
import at.ac.univie.isc.asio.security.AllowAllVerifier;
import at.ac.univie.isc.asio.security.NoopTrustManager;
import at.ac.univie.isc.asio.sql.Database;
import at.ac.univie.isc.asio.web.HttpServer;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.google.common.net.HttpHeaders;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.EncoderConfig;
import com.jayway.restassured.config.HttpClientConfig;
import com.jayway.restassured.config.MatcherConfig;
import com.jayway.restassured.config.SSLConfig;
import com.jayway.restassured.filter.log.LogDetail;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.params.CoreConnectionPNames;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.functions.Action0;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.assumeThat;

/**
 * Base for integration tests. Prepares RestAssured config and global rules. Tests can be adapted to
 * deployment specific configuration by manipulating the static fields.
 */
@Category(Integration.class)
@RunWith(HierarchicalContextRunner.class)
public abstract class IntegrationTest {

  @BeforeClass
  public static void restAssuredDefaults() {
    RestAssured.config = RestAssured.config()
        .httpClient(HttpClientConfig.httpClientConfig()
            .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 30_000)
            .setParam(CoreConnectionPNames.SO_TIMEOUT, 30_000)
        )
        .encoderConfig(EncoderConfig.encoderConfig()
            .defaultContentCharset(Charsets.UTF_8.name()).and()
            .defaultQueryParameterCharset(Charsets.UTF_8.name()).and()
            .appendDefaultContentCharsetToContentTypeIfUndefined(false)
        )
        .matcherConfig(
            new MatcherConfig().errorDescriptionType(MatcherConfig.ErrorDescriptionType.HAMCREST)
        )
        .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
    ;
  }

  @BeforeClass
  public static void validateConfiguration() {
    requireNonNull(config, "Global configuration missing. Use IntegrationTest.configure(..) to provide settings.");
    config.validate();
  }

  // === global scope ==============================================================================

  private static final IntegrationSettings config = new IntegrationSettings();

  /**
   * Configure the integration tests.
   */
  public static IntegrationSettings configure() {
    return config;
  }

  public static HttpServer atos = FakeAtosService.attachTo(HttpServer.create("atos-fake")).start(0);

  /**
   * Create dsl from the current global settings.
   */
  private static IntegrationDsl init(final Interactions interactions) {
    restAssuredDefaults();
    validateConfiguration();
    return new IntegrationDsl(new RequestSpecAssembler(config, interactions)).copy(config.dslDefaults);
  }

  /**
   * Deploy test container.
   */
  public static void deploy(final String name, final ByteSource mapping) {
    init(Interactions.empty()).manage()
      .and()
        .header(HttpHeaders.ACCEPT, "application/json")
        .contentType("text/turtle")
        .content(Payload.asArray(mapping))
        .log().ifValidationFails(LogDetail.ALL)
      .when()
        .put("container/{schema}", name)
      .then()
        .log().ifValidationFails(LogDetail.ALL)
      .statusCode(HttpStatus.SC_CREATED);
  }

  /**
   * Ensure the default schema is up and trigger lazy initialization of server components to avoid
   * timeouts in test cases.
   */
  public static void warmup() {
    // TODO : check supported languages when implemented
    init(Interactions.empty())
        .role("read").and()
        .log().ifValidationFails(LogDetail.ALL)
      .get()
      .then()
        .statusCode(HttpStatus.SC_OK)
        .log().ifValidationFails(LogDetail.ALL);
  }

  // === test case scope ===========================================================================

  @Rule
  public Timeout timeout = Rules.timeout(config.timeoutInSeconds, TimeUnit.SECONDS);

  @Rule
  public Interactions interactions = Rules.interactions().and(atos);

  private final IntegrationDsl dsl = init(interactions);

  // === endpoints =================================================================================

  protected final URI eventsEndpoint() {
    return config.serviceBase.resolve(config.managementService).resolve(config.eventService);
  }

  // === test components ===========================================================================

  /**
   * @return the backing test database, if available
   * @throws AssertionError if no database is available
   */
  protected final Database database() {
    if (config.database == null) { throw new AssertionError("missing database configuration"); }
    return config.database;
  }

  /**
   * @return an observable yielding all events from the server on subscription
   */
  protected final Observable<InboundEvent> eventStream() {
    final ClientBuilder clientConfig = ClientBuilder.newBuilder();
    Unchecked.run(new Unchecked.Action() {
      @Override
      public void call() throws Exception {
        final SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(null, NoopTrustManager.asArray(), null);
        clientConfig.hostnameVerifier(AllowAllVerifier.instance()).sslContext(ssl);
      }
    });
    final Client client = clientConfig.build();

    final UsernamePasswordCredentials credentials = config.rootCredentials;
    final HttpAuthenticationFeature authentication =
        HttpAuthenticationFeature.basic(credentials.getUserName(), credentials.getPassword());
    client.register(authentication);

    return EventStream
        .listenTo(EventSource.target(client.target(eventsEndpoint())).build())
        .doOnEach(interactions.attached(new EventReport()))
        .finallyDo(new Action0() {
          @Override
          public void call() {
            client.close();
          }
        });
  }

  // === assumptions ===============================================================================

  /**
   * Skip test if no endpoint for the given language exists.
   *
   * @param language name of required language
   */
  protected final void ensureLanguageSupported(final String language) {
    final int responseCode =
        given().role("admin").spec().get("/{language}", language).then().extract().statusCode();
    assumeThat(language + " not supported", responseCode, not(HttpStatus.SC_NOT_FOUND));
  }

  /**
   * Skip test if the service is not enforcing authorization, e.g. uses a fixed permission.
   */
  protected final void ensureSecured() {
    assumeThat("service is not secured", config.auth.isAuthorizing(), equalTo(true));
  }

  /**
   * Skip test if the backing database is not configured, e.g. there is none.
   */
  protected final void ensureDatabaseAccessible() {
    assumeThat("database not accessible", config.database, not(nullValue()));
  }

  protected final void ensureContainerSupported() {
    final int responseCode =
        given().manage().spec().get("/container").then().extract().statusCode();
    assumeThat("container not supported", responseCode, not(HttpStatus.SC_NOT_FOUND));
  }

  // === fluent test dsl ===========================================================================

  /**
   * Delegate to fluent dsl.
   */
  protected final IntegrationDsl given() {
    return dsl;
  }
}
