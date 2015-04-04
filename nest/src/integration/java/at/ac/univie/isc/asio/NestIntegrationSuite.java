package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.io.TransientFile;
import at.ac.univie.isc.asio.security.AuthMechanism;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.spring.ApplicationRunner;
import at.ac.univie.isc.asio.sql.Database;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.net.URI;

@RunWith(Suite.class)
@Suite.SuiteClasses({AllFeatures.class})
public class NestIntegrationSuite {
  @ClassRule
  public static ApplicationRunner application = ApplicationRunner.run(Nest.class);
  @ClassRule
  public static TransientFile keystore = TransientFile.from(Classpath.load("keystore.integration"));

  @BeforeClass
  public static void start() {
    final Database h2 = Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));

    final String[] args = new String[] {
        "--server.ssl.key-store=" + keystore.path(),
        "--asio.feature.vph-metadata=on",
        "--asio.metadata-repository=" + IntegrationTest.atos.address(),
    };
    application.profile("std").run(args);

    IntegrationTest.configure()
        .baseService(URI.create("https://localhost:" + application.getPort() + "/"))
        .auth(AuthMechanism.basic("root", "change").overrideCredentialDelegationHeader("Delegate-Authorization"))
        .rootCredentials("root", "change")
        .database(h2)
        .defaults().schema("public").role(Role.NONE.name());

    IntegrationTest.deploy("public", Classpath.load("config.integration.ttl"));
  }
}
