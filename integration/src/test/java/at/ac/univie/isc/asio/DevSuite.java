package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.sql.Database;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.SSLConfig;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.net.URI;

@RunWith(Suite.class)
@Suite.SuiteClasses
    ({
        FeatureMetadata.class,
        FeatureProtocol.class,
        FeatureEvents.class,
        FeatureSql.class,
        FeatureSparql.class,
        FeatureSparqlFederation.class,
        FeatureCredentialDelegation.class,
        ReferenceSql.class,
        ReferenceSparql.class,
    })
public class DevSuite {
  @BeforeClass
  public static void init() {
    IntegrationTest.configure()
        .baseService(URI.create("https://localhost:8443/asio/"))
        .auth(AuthMechanism.uri())
        .database(Database.create("jdbc:h2:tcp://localhost/mem:test").credentials("root", "change").build());
    RestAssured.config =
        RestAssured.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
  }
}
