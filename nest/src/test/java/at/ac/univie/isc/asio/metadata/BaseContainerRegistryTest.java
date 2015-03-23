package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.container.CatalogEvent;
import at.ac.univie.isc.asio.container.Container;
import at.ac.univie.isc.asio.container.StubContainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class BaseContainerRegistryTest {
  @Rule
  public final ExpectedException error = ExpectedException.none();

  private final BaseContainerRegistry subject = new BaseContainerRegistry() { /* empty */ };

  @Test
  public void should_fail_if_requested_schema_not_present() throws Exception {
    error.expect(Schema.NotFound.class);
    error.expectMessage(containsString("not-there"));
    subject.find(Schema.valueOf("not-there"));
  }

  @Test
  public void should_find_deployed_schema() throws Exception {
    final Container expected = StubContainer.create("test");
    subject.onDeploy(new CatalogEvent.SchemaDeployed(expected));
    assertThat(subject.find(Schema.valueOf("test")), sameInstance(expected));
  }

  @Test
  public void should_not_find_schema_after_dropping_it() throws Exception {
    final Container expected = StubContainer.create("test");
    subject.onDeploy(new CatalogEvent.SchemaDeployed(expected));
    subject.onDrop(new CatalogEvent.SchemaDropped(expected));
    error.expect(Schema.NotFound.class);
    subject.find(Schema.valueOf("test"));
  }
}
