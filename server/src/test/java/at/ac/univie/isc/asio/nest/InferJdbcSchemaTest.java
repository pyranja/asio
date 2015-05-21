/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
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
  public void should_leave_schema_blank_if_no_jdbc_or_dataset_name_found() throws Exception {
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", nullValue()));
  }

  @Test
  public void should_use_dataset_name_if_jdbc_url_missing() throws Exception {
    input.getDataset().setName(Id.valueOf("test"));
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", equalTo("test")));
  }

  @Test
  public void should_use_dataset_name_if_jdbc_url_unknown() throws Exception {
    input.getDataset().setName(Id.valueOf("test"));
    input.getJdbc().setUrl("jdbc:h2:mem");
    assertThat(subject.apply(input).getJdbc(), hasProperty("schema", equalTo("test")));
  }
}
