package at.ac.univie.isc.asio.nest;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class InferJdbcSchemaTest {
  private final NestConfig input = NestConfig.empty();
  private final InferJdbcSchema subject = new InferJdbcSchema();

  @Test
  public void should_leave_non_null_schema_in_place() throws Exception {
    input.getJdbc().setSchema("test").setUrl("jdbc:mysql:///other");
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", equalTo("test")));
  }

  @Test
  public void should_replace_null_schema_with_inferred() throws Exception {
    input.getJdbc().setUrl("jdbc:mysql:///inferred");
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", equalTo("inferred")));
  }

  @Test
  public void should_leave_schema_blank_if_jdbc_url_missing() throws Exception {
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", nullValue()));
  }

  @Test
  public void should_leave_schema_blank_if_jdbc_url_unknown() throws Exception {
    input.getJdbc().setUrl("jdbc:h2:mem");
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", nullValue()));
  }
}
