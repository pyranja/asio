package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.InvalidUsage;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class InferJdbcDriverTest {

  private final NestConfig config = NestConfig.empty();
  private final InferJdbcDriver subject = new InferJdbcDriver();

  @Test
  public void should_keep_explicit_jdbc_driver() throws Exception {
    config.getJdbc().setDriver("org.example.Driver");
    subject.apply(config);
    assertThat(config.getJdbc().getDriver(), equalTo("org.example.Driver"));
  }

  @Test
  public void should_set_driver_if_missing_and_able_to_infer_it() throws Exception {
    config.getJdbc().setUrl("jdbc:mysql://localhost/");
    subject.apply(config);
    assertThat(config.getJdbc().getDriver(), equalTo("com.mysql.jdbc.Driver"));
  }

  @Test(expected = InvalidUsage.class)
  public void should_fail_fast_if_driver_unknown() throws Exception {
    config.getJdbc().setUrl("jdbc:unknown:test");
    subject.apply(config);
  }

  @Test(expected = InvalidUsage.class)
  public void should_fail_fast_if_driver_and_url_missing() throws Exception {
    subject.apply(config);
  }
}
