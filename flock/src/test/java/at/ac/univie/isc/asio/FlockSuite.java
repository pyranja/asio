package at.ac.univie.isc.asio;

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
//    FeatureEvents.class,  // listens to wrong endpoint /meta instead of /insight
    FeatureSparql.class,
    FeatureSparqlFederation.class,
    FeatureCredentialDelegation.class,
})
public class FlockSuite {
  private static ApplicationContext asio;

  @BeforeClass
  public static void start() {
    asio = Flock.APPLICATION.run("--server.port=0");
    // FIXME : use application listener to get hold of the embedded container
    // FIXME : move init code to a rule ?
    final int port =
        ((AnnotationConfigEmbeddedWebApplicationContext) asio).getEmbeddedServletContainer().getPort();
    IntegrationTest.asio =
        AsioSpec.withoutAuthorization(URI.create("http://localhost:" + port + "/"));
    RestAssured.config =
        RestAssured.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
  }

  @AfterClass
  public static void stop() {
    SpringApplication.exit(asio);
  }
}
