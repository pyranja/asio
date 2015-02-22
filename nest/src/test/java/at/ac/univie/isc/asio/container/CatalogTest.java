package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.CaptureEvents;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
public class CatalogTest {
  private final CaptureEvents<Catalog.Event> events = CaptureEvents.create(Catalog.Event.class);
  private Catalog subject = new Catalog(events.bus());

  @Test
  public void found_schemas_are_a_snapshot() throws Exception {
    final Collection<Schema> schemas = Arrays.asList(SimpleSchema.create("1"), SimpleSchema.create("2"));
    for (Schema schema : schemas) {
      subject.deploy(schema);
    }
    final Collection<Schema> all = subject.findAll();
    assertThat(all, contains(schemas.toArray()));
    subject.drop("1");  // catalog changes, but snapshot should not
    assertThat(all, contains(schemas.toArray()));
  }

  @Test
  public void no_former_if_deploying_new() throws Exception {
    final Optional<Schema> former = subject.deploy(SimpleSchema.create("test"));
    assertThat(former.isPresent(), is(false));
  }

  @Test
  public void yield_replaced_if_replacing() throws Exception {
    final Schema replaced = SimpleSchema.create("test");
    subject.deploy(replaced);
    final Schema replacement = SimpleSchema.create("test");
    final Optional<Schema> former = subject.deploy(replacement);
    assertThat(former.get(), is(replaced));
  }

  @Test
  public void yield_nothing_if_dropping_missing() throws Exception {
    final Optional<Schema> dropped = subject.drop("name");
    assertThat(dropped.isPresent(), is(false));
  }

  @Test
  public void yield_dropped() throws Exception {
    final Schema schema = SimpleSchema.create("name");
    subject.deploy(schema);
    final Optional<Schema> dropped = subject.drop("name");
    assertThat(dropped.get(), is(schema));
  }

  @Test
  public void emit_event_on_deploying_schema() throws Exception {
    subject.deploy(SimpleSchema.create("name"));
    assertThat(events.captured(), contains(instanceOf(Catalog.SchemaDeployed.class)));
  }

  @Test
  public void emit_event_on_dropping_schema() throws Exception {
    subject.deploy(SimpleSchema.create("name"));
    subject.drop("name");
    assertThat(events.captured(), contains(instanceOf(Catalog.SchemaDeployed.class), instanceOf(Catalog.SchemaDropped.class)));
  }

  @Test
  public void emit_drop_and_deploy_when_replacing() throws Exception {
    subject.deploy(SimpleSchema.create("first"));
    subject.deploy(SimpleSchema.create("first"));
    assertThat(events.captured(), contains(instanceOf(Catalog.SchemaDeployed.class), instanceOf(Catalog.SchemaDropped.class), instanceOf(Catalog.SchemaDeployed.class)));
  }

  @Test
  public void no_event_if_schema_not_present_on_drop() throws Exception {
    subject.drop("name");
    assertThat(events.captured(), is(emptyIterable()));
  }
}
