package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.d2rq.lang.Database;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FindJdbcConfigTest {

  private final FindJdbcConfig subject = FindJdbcConfig.using(TimeoutSpec.undefined());
  private final Database db = new Database(ResourceFactory.createResource());

  @Before
  public void injectTestUrlAndDriver() {
    db.setJdbcURL("test-url");
    db.setJDBCDriver("test-driver");
  }

  @Test
  public void finds_configured_jdbc_url() throws Exception {
    subject.visit(db);
    assertThat(subject.single().getUrl(), is("test-url"));
  }

  @Test
  public void finds_configured_driver() {
    subject.visit(db);
    assertThat(subject.single().getDriver(), is("test-driver"));
  }

  @Test
  public void uses_injected_timeout() throws Exception {
    subject.visit(db);
    assertThat(subject.single().getTimeout(), is(TimeoutSpec.undefined()));
  }

  @Test
  public void finds_configured_credentials() throws Exception {
    db.setUsername("test-user");
    db.setPassword("test-password");
    subject.visit(db);
    assertThat(subject.single().getCredentials(), is(Identity.from("test-user", "test-password")));
  }
}
