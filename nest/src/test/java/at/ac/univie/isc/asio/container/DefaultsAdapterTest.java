package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class DefaultsAdapterTest {
  @Test
  public void should_yield_a_cloned_settings_object() throws Exception {
    final ContainerSettings original = ContainerSettings.of(Schema.DEFAULT);
    original.setIdentifier("identifier");
    original.setTimeout(100L);
    final ContainerSettings.Sparql sparql = new ContainerSettings.Sparql();
    sparql.setFederation(true);
    sparql.setD2rBaseUri(URI.create("http://example.com"));
    sparql.setD2rMappingLocation(URI.create("file:///mapping.ttl"));
    original.setSparql(sparql);
    final ContainerSettings.Datasource datasource = new ContainerSettings.Datasource();
    datasource.setJdbcUrl("jdbc:mysql");
    datasource.setUsername("username");
    datasource.setPassword("password");
    original.setDatasource(datasource);
    final DefaultsAdapter subject = DefaultsAdapter.from(original);
    final ContainerSettings translated = subject.translate(Schema.DEFAULT, null);
    assertThat(translated, not(sameInstance(original)));
    assertThat(translated.asMap(), equalTo(original.asMap()));
  }
}
