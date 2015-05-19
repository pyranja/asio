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
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Table;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.tool.Timeout;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import rx.Observable;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@ContextHierarchy({
    @ContextConfiguration(name = "parent", classes = MinimalConfigWiringTest.MinimalConfig.class),
    @ContextConfiguration(name = "subject", classes = NestBluePrint.class)
})
public class MinimalConfigWiringTest extends BaseContainerWiring {

  @Configuration
  @Profile("wiring-test")
  @Import(BaseContainerWiring.BaseWiringConfig.class)
  public static class MinimalConfig {
    @Bean
    public Dataset datasetConfig() {
      return new Dataset()
          .setName(Id.valueOf("test"))
          .setIdentifier(URI.create("urn:asio:minimal"))
          .setTimeout(Timeout.undefined())
          .setFederationEnabled(true);
    }

    @Bean
    public Jdbc jdbcConfig() {
      return new Jdbc().setUrl("jdbc:h2:mem:").setSchema("public")
          .addProperty("INIT", "CREATE TABLE test(id INT NOT NULL)");
    }
  }

  @Test
  public void should_define_definition_using_local_datasource() throws Exception {
    final Observable<SqlSchema> definition =
        applicationContext.getBean(NestBluePrint.BEAN_DEFINITION_SOURCE, Observable.class);
    final SqlSchema result = definition.toBlocking().single();
    final Table table = result.getTable().iterator().next();
    assertThat(table.getName(), equalToIgnoringCase("test"));
    assertThat(table.getColumn().iterator().next().getName(), equalToIgnoringCase("id"));
  }

  @Test
  public void should_define_empty_metadata() throws Exception {
    final Observable<SchemaDescriptor> metadata =
        applicationContext.getBean(NestBluePrint.BEAN_DESCRIPTOR_SOURCE, Observable.class);
    assertThat(metadata.isEmpty().toBlocking().single(), equalTo(true));
  }

  @Test
  public void should_define_HikariDatasource_with_default_settings() throws Exception {
    final HikariConfig hikari = applicationContext.getBean(HikariDataSource.class);
    assertThat(hikari.getJdbcUrl(), equalTo("jdbc:h2:mem:"));
    assertThat(hikari.getUsername(), isEmptyString());
    assertThat(hikari.getPassword(), isEmptyString());
    assertThat(hikari.getConnectionTimeout(), equalTo(100L));
  }

  @Test
  public void should_use_global_timeout() throws Exception {
    assertThat(applicationContext.getBean(Timeout.class),
        equalTo(Timeout.from(100, TimeUnit.MILLISECONDS)));
  }
}
