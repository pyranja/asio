package at.ac.univie.isc.asio.tool;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class JdbcToolsTest {

  public static class IsValidJdbcUrl {
    @Test
    public void should_accept_mysql_jdbc_url() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl("jdbc:mysql://localhost/test"), equalTo(true));
    }

    @Test
    public void should_reject_null_as_jdbc_url() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl(null), equalTo(false));
    }

    @Test
    public void should_reject_url_if_jdbc_prefix_missing() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl("some-text"), equalTo(false));
    }

    @Test
    public void should_accept_h2_jdbc_url() throws Exception {
      assertThat(JdbcTools.isValidJdbcUrl("jdbc:h2:mem:test"), equalTo(true));
    }
  }

  public static class InferJdbcDriver {
    @Test
    public void should_return_absent_if_driver_unknown() throws Exception {
      assertThat(JdbcTools.inferDriverClass("jdbc:test://localhost/").isPresent(), equalTo(false));
    }

    @Test
    public void should_find_mysql_driver() throws Exception {
      assertThat(JdbcTools.inferDriverClass("jdbc:mysql://localhost/").get(),
          equalTo("com.mysql.jdbc.Driver"));
    }

    @Test
    public void should_find_h2_driver() throws Exception {
      assertThat(JdbcTools.inferDriverClass("jdbc:h2:mem").get(),
          equalTo("org.h2.Driver"));
    }

    @Test
    public void should_return_absent_if_url_null() throws Exception {
      assertThat(JdbcTools.inferDriverClass(null).isPresent(), equalTo(false));
    }
  }

  public static class InjectRequiredJdbcProperties {
    @Test
    public void should_force_h2_properties() throws Exception {
      final Map<String, String> original = new HashMap<>();
      original.put("MODE", "wrong");
      original.put("DATABASE_TO_UPPER", "true");
      final Properties injected = JdbcTools.injectRequiredProperties(original, "jdbc:h2:mem");
      assertThat(injected, has("MODE", "MYSQL"));
      assertThat(injected, has("DATABASE_TO_UPPER", "false"));
    }

    private Matcher<? super Map<?,?>> has(final String key, final String value) {
      return Matchers.<Object, Object>hasEntry(key, value);
    }
  }
}
