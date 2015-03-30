package at.ac.univie.isc.asio.container.nest;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.container.DefinitionService;
import at.ac.univie.isc.asio.container.DescriptorService;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.zaxxer.hikari.HikariConfig;
import org.d2rq.db.SQLConnection;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import rx.Observable;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@ContextHierarchy({
    @ContextConfiguration(name = "parent", classes = OverrideConfigWiringTest.OverridingConfig.class),
    @ContextConfiguration(name = "subject", classes = NestBluePrint.class)
})
public class OverrideConfigWiringTest extends BaseContainerWiring {
  private static final SchemaDescriptor SCHEMA_DESCRIPTOR = SchemaDescriptor.empty("test").build();
  private static final SqlSchema SCHEMA_DEFINITION = new SqlSchema();

  @Configuration
  @Profile("wiring-test")
  public static class OverridingConfig extends BaseContainerWiring.BaseWiringConfig {

    @Bean
    public DescriptorService metadataService() {
      return new DescriptorService() {
        @Override
        public Observable<SchemaDescriptor> metadata(final URI identifier) {
          return Observable.just(SCHEMA_DESCRIPTOR);
        }
      };
    }

    @Bean
    public DefinitionService definitionService() {
      return new DefinitionService() {
        @Override
        public Observable<SqlSchema> definition(final String name) {
          return Observable.just(SCHEMA_DEFINITION);
        }
      };
    }

    @Bean
    public Dataset datasetConfig() {
      return new Dataset()
          .setName(Id.valueOf("test"))
          .setIdentifier(URI.create("urn:asio:test"))
          .setTimeout(TimeoutSpec.from(1000, TimeUnit.SECONDS))
          .setFederationEnabled(false);
    }

    @Bean
    public Jdbc jdbcConfig() {
      return new Jdbc()
          .setUrl("jdbc:h2:mem:")
          .setDriver("org.h2.Driver")
          .setUsername("root")
          .setPassword("change")
          .setSchema("schema");
    }
  }

  @Test
  public void should_define_definition_using_container_service() throws Exception {
    final Observable definition =
        applicationContext.getBean(NestBluePrint.BEAN_DEFINITION_SOURCE, Observable.class);
    assertThat(definition.toBlocking().single(), Matchers.<Object>equalTo(SCHEMA_DEFINITION));
  }

  @Test
  public void should_define_metadata_using_container_service() throws Exception {
    final Observable metadata =
        applicationContext.getBean(NestBluePrint.BEAN_DESCRIPTOR_SOURCE, Observable.class);
    assertThat(metadata.toBlocking().single(), Matchers.<Object>equalTo(SCHEMA_DESCRIPTOR));
  }

  @Test
  public void should_define_HikariConfig_with_given_settings() throws Exception {
    final HikariConfig hikari = applicationContext.getBean(HikariConfig.class);
    assertThat(hikari.getJdbcUrl(), equalTo("jdbc:h2:mem:"));
    assertThat(hikari.getCatalog(), equalTo("schema"));
    assertThat(hikari.getUsername(), equalTo("root"));
    assertThat(hikari.getPassword(), equalTo("change"));
    assertThat(hikari.getConnectionTimeout(), equalTo(1_000_000L));
  }

  @Test
  public void should_override_SQLConnection_with_given_settings() throws Exception {
    final SQLConnection conn = applicationContext.getBean(SQLConnection.class);
    assertThat(conn.getJdbcURL(), equalTo("jdbc:h2:mem:"));
    assertThat(conn.getJdbcDriverClass(), equalTo("org.h2.Driver"));
    assertThat(conn.getUsername(), equalTo("root"));
    assertThat(conn.getPassword(), equalTo("change"));
  }

  @Test
  public void should_override_global_timeout() throws Exception {
    assertThat(applicationContext.getBean(TimeoutSpec.class),
        equalTo(TimeoutSpec.from(1000, TimeUnit.SECONDS)));
  }
}
