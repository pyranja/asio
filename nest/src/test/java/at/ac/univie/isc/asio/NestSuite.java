package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.integration.IntegrationTest;
import at.ac.univie.isc.asio.io.Classpath;
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
@Suite.SuiteClasses({
    FeatureProtocol.class,
    FeatureMetadata.class,
    FeatureEvents.class,
    FeatureSql.class,
    FeatureSparql.class,
    FeatureSparqlFederation.class,
    FeatureCredentialDelegation.class,
    ReferenceSql.class,
    ReferenceSparql.class,
})
public class NestSuite {
  @ClassRule
  public static ApplicationRunner application = ApplicationRunner.run(Nest.class);

  @BeforeClass
  public static void start() {
    final Database h2 = Database.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        .credentials("root", "change").build()
        .execute(Classpath.read("sql/database.integration.sql"));
    application.run();

    IntegrationTest.configure()
        .baseService(URI.create("http://localhost:" + application.getPort() + "/catalog/"))
        .auth(AuthMechanism.basic(application.property("asio.secret", String.class))
            .overrideCredentialDelegationHeader("Delegate-Authorization"))
        .database(h2)
        .defaults().schema("public").role(Role.NONE.name());
  }
}
