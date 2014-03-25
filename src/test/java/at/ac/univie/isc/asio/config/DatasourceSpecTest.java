package at.ac.univie.isc.asio.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DatasourceSpecTest {

  @Test
  public void should_use_empty_password_if_null_given() throws Exception {
    final DatasourceSpec spec =
        DatasourceSpec.connectTo("test-url", "test-driver").authenticateAs("test-user", null);
    assertThat(spec.getPassword(), is(""));
  }
}
