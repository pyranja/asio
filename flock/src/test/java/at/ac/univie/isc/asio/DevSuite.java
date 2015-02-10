package at.ac.univie.isc.asio;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.net.URI;

@Ignore("intended for use from IDE")
@RunWith(Suite.class)
@Suite.SuiteClasses
    ({
        SparqlProtocolTest.class
        , SparqlFederationTest.class
    })
public class DevSuite {
  @BeforeClass
  public static void init() {
    EnvironmentSpec.create(URI.create("http://localhost:8080/")).sparql(URI.create("sparql"));
  }
}
