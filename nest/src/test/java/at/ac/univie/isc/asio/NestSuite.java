package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.io.Classpath;
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
      final URI root = URI.create("http://localhost:" + application.getPort() + "/catalog/");
      IntegrationTest.asio = AsioSpec
          .withBasicAuthorization(root, application.property("asio.secret", String.class))
          .useDelegateCredentialsHeader("Delegate-Authorization")
          .useSchema("public");
      IntegrationTest.database = h2;
    }
}
