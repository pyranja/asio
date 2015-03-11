package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.security.AuthMechanism;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.SSLConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;

import java.net.URI;

import static org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    FeatureProtocol.class,
    FeatureSparql.class,
    FeatureSparqlFederation.class,
    FeatureCredentialDelegation.class,
    FeatureEvents.class,
})
public class FlockIntegrationSuite {
  private static ApplicationContext asio;

  @BeforeClass
  public static void start() {
    asio = Flock.APPLICATION.run("--server.port=0");
    // FIXME : use application listener to get hold of the embedded container
    // FIXME : move init code to a rule ?
    final int port =
        ((AnnotationConfigEmbeddedWebApplicationContext) asio).getEmbeddedServletContainer().getPort();
    IntegrationTest.configure()
        .baseService(URI.create("http://localhost:" + port + "/"))
        .managementService(URI.create("insight/"))
        .auth(AuthMechanism.none());
    RestAssured.config =
        RestAssured.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
  }

  @AfterClass
  public static void stop() {
    SpringApplication.exit(asio);
  }
}
