package at.ac.univie.isc.asio.d2rq;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FindJdbcConfigTest {

  private final D2rqContainerAdapter.FindJdbcConfig subject =
      D2rqContainerAdapter.FindJdbcConfig.create("default");
  private final Database db = new Database(ResourceFactory.createResource());

  @Before
  public void injectTestUrlAndDriver() {
    db.setJdbcURL("test-url");
    db.setJDBCDriver("test-driver");
  }

  @Test
  public void finds_configured_jdbc_url() throws Exception {
    subject.visit(db);
    assertThat(subject.datasource.getJdbcUrl(), is("test-url"));
  }

  @Test
  public void finds_configured_credentials() throws Exception {
    db.setUsername("test-user");
    db.setPassword("test-password");
    subject.visit(db);
    assertThat(subject.datasource.getUsername(), is("test-user"));
    assertThat(subject.datasource.getPassword(), is("test-password"));
  }

  @Test
  public void should_use_default_schema_if_not_specified() throws Exception {
    subject.visit(db);
    assertThat(subject.datasource.getSchema(), equalTo("default"));
  }

  @Test
  public void finds_configured_schema() throws Exception {
    db.setConnectionProperty("schema", "configured");
    subject.visit(db);
    assertThat(subject.datasource.getSchema(), equalTo("configured"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_multiple_configurations() throws Exception {
    subject.visit(db);
    subject.visit(db);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_config() throws Exception {
    subject.parse(new Mapping());
  }
}
