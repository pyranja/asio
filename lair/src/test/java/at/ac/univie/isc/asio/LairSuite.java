package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.sql.Database;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.SSLConfig;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.net.URI;

import static org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
//    FeatureMetadata.class,
    FeatureProtocol.class,
//    FeatureEvents.class,
    FeatureSql.class,
    FeatureSparql.class,
    FeatureSparqlFederation.class,
    FeatureCredentialDelegation.class,
    ReferenceSparql.class,
//    ReferenceSql.class,
})
public class LairSuite {
  @BeforeClass
  public static void start() {
    IntegrationTest.asio =
        AsioSpec.withUriAuthorization(URI.create("https://localhost:8443/asio/"));
    IntegrationTest.database =
        Database.create("jdbc:h2:tcp://localhost/mem:test").credentials("root", "change").build();
    RestAssured.config = RestAssured.config()
        .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
  }
}