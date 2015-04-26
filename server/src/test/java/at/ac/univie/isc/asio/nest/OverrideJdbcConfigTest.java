package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.database.Jdbc;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OverrideJdbcConfigTest {

  @Test
  public void should_keep_dataset() throws Exception {
    final OverrideJdbcConfig subject = new OverrideJdbcConfig(new Jdbc());
    final NestConfig initial = NestConfig.empty();
    final NestConfig processed = subject.apply(initial);
    assertThat(processed.getDataset(), sameInstance(initial.getDataset()));
  }

  @Test
  public void should_replace_jdbc_connection_props_with_global() throws Exception {
    final Map<String, String> props = new HashMap<>();
    props.put("key", "value");
    final Jdbc override = new Jdbc()
        .setUrl("jdbc:asio:override")
        .setDriver("MyJdbcDriver")
        .setUsername("override-username")
        .setPassword("override-password")
        .setProperties(props);
    final OverrideJdbcConfig subject = new OverrideJdbcConfig(override);
    final Jdbc processed = subject.apply(NestConfig.empty()).getJdbc();
    assertThat(processed.getUrl(), equalTo("jdbc:asio:override"));
    assertThat(processed.getDriver(), equalTo("MyJdbcDriver"));
    assertThat(processed.getUsername(), equalTo("override-username"));
    assertThat(processed.getPassword(), equalTo("override-password"));
    assertThat(processed.getProperties(), equalTo(props));
  }

  @Test
  public void should_not_use_the_mutable_global_instance_as_override() throws Exception {
    final Jdbc override = new Jdbc();
    final Jdbc processed = new OverrideJdbcConfig(override).apply(NestConfig.empty()).getJdbc();
    assertThat(processed, not(sameInstance(override)));
  }

  @Test
  public void should_not_override_schema() throws Exception {
    final Jdbc override = new Jdbc().setSchema("override");
    final NestConfig initial = NestConfig.empty();
    initial.getJdbc().setSchema("original");
    final NestConfig processed = new OverrideJdbcConfig(override).apply(initial);
    assertThat(processed.getJdbc().getSchema(), equalTo("original"));
  }
}
