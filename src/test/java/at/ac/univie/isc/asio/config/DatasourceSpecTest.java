package at.ac.univie.isc.asio.config;

import org.junit.Test;

import static at.ac.univie.isc.asio.config.DatasourceSpec.connectTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;

public class DatasourceSpecTest {

  private DatasourceSpec spec;

  @Test
  public void should_build_correct_spec() throws Exception {
    spec = connectTo("jdbcUrl").with("jdbcDriver").authenticateAs("user", "password");
    assertThat(spec.getJdbcUrl(), is("jdbcUrl"));
    assertThat(spec.getJdbcDriver(), is("jdbcDriver"));
    assertThat(spec.getUsername(), is("user"));
    assertThat(spec.getPassword(), is("password"));
  }

  @Test
  public void should_use_empty_password_if_null_given() throws Exception {
    spec = connectTo("test-url").with("test-driver").authenticateAs("test-user", null);
    assertThat(spec.getPassword(), is(""));
  }

  @Test
  public void should_use_empty_user_if_null_given() throws Exception {
    spec = connectTo("test-url").with("test-driver").authenticateAs(null, "test-pw");
    assertThat(spec.getUsername(), isEmptyString());
  }

  @Test
  public void should_leave_jdbc_url_as_is_if_no_default_options_necessary() throws Exception {
    spec = connectTo("jdbc:genericdb://example.com/test").with("org.example.Driver").authenticateAs(null, null);
    assertThat(spec.getJdbcUrl(), is("jdbc:genericdb://example.com/test"));
  }

  @Test
  public void should_append_mysql_default_options() throws Exception {
    spec = connectTo("jdbc:mysql://example.com/test").with("org.example.Driver").anonymous();
    assertThat(spec.getJdbcUrl(), is("jdbc:mysql://example.com/test?zeroDateTimeBehavior=convertToNull"));
  }
}
