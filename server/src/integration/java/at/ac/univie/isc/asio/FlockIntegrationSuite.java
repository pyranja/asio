package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.spring.ApplicationRunner;
import org.apache.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.net.URI;

@RunWith(Suite.class)
@Suite.SuiteClasses(AllFeatures.class)
public class FlockIntegrationSuite {
  @ClassRule
  public static ApplicationRunner application = ApplicationRunner.run(Asio.class);

  @BeforeClass
  public static void start() {
    final String[] args = new String[] {
        "--asio.metadata-repository=" + IntegrationTest.atos.address()
    };
    application.profile("flock-test").run(args);

    IntegrationTest.configure()
        .baseService(URI.create("http://localhost:" + application.getPort() + "/"))
        .auth(AuthMechanism.none().overrideCredentialDelegationHeader(HttpHeaders.AUTHORIZATION))
        .rootCredentials("root", "change")
        .timeoutInSeconds(10)
        .defaults().noSchema().role(Role.NONE.name());

    IntegrationTest.warmup();
  }
}
