package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.spring.ApplicationRunner;
import at.ac.univie.isc.asio.sql.Database;
import org.apache.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.net.URI;

/**
 * Run nest integration tests using the URI-based auth mechanism required for VPH deployments.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(AllFeatures.class)
public class VphIntegrationSuite {
  @ClassRule
  public static ApplicationRunner application = ApplicationRunner.run(Nest.class);

  @BeforeClass
  public static void start() throws IOException {
    final Database h2 = Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));

    final String[] args = new String[] {
        "--asio.metadata-repository=" + IntegrationTest.atos.address()
    };
    application.profile("vph").run(args);

    IntegrationTest.configure()
        .baseService(URI.create("http://localhost:" + application.getPort() + "/"))
        .auth(AuthMechanism.uri().overrideCredentialDelegationHeader(HttpHeaders.AUTHORIZATION))
        .rootCredentials("root", "change")
        .database(h2)
        .timeoutInSeconds(10)
        .defaults().schema("public").role(Role.NONE.name());

    IntegrationTest.deploy("public", Classpath.load("config.integration.ttl"));
  }
}
