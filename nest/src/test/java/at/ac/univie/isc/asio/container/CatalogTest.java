package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.CaptureEvents;
import at.ac.univie.isc.asio.Schema;
import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
public class CatalogTest {
  @Rule
  public ExpectedException error = ExpectedException.none();

  private final CaptureEvents<CatalogEvent> events = CaptureEvents.create(CatalogEvent.class);
  private Catalog<Container> subject = new Catalog<>(events.bus());

  @Test
  public void no_former_if_deploying_new() throws Exception {
    final Optional<Container> former = subject.deploy(DummySchema.create("test"));
    assertThat(former.isPresent(), is(false));
  }

  @Test
  public void yield_replaced_if_replacing() throws Exception {
    final Container replaced = DummySchema.create("test");
    subject.deploy(replaced);
    final Container replacement = DummySchema.create("test");
    final Optional<Container> former = subject.deploy(replacement);
    assertThat(former.get(), is(replaced));
  }

  @Test
  public void yield_nothing_if_dropping_missing() throws Exception {
    final Optional<Container> dropped = subject.drop(Schema.valueOf("name"));
    assertThat(dropped.isPresent(), is(false));
  }

  @Test
  public void yield_dropped() throws Exception {
    final Container container = DummySchema.create("name");
    subject.deploy(container);
    final Optional<Container> dropped = subject.drop(Schema.valueOf("name"));
    assertThat(dropped.get(), is(container));
  }

  @Test
  public void reject_deploy_after_close() throws Exception {
    subject.clear();
    error.expect(IllegalStateException.class);
    subject.deploy(DummySchema.create("test"));
  }

  @Test
  public void reject_drop_after_close() throws Exception {
    subject.clear();
    error.expect(IllegalStateException.class);
    subject.drop(Schema.valueOf("test"));
  }

  @Test
  public void should_yield_remaining_after_close() throws Exception {
    final Container container = DummySchema.create("test");
    subject.deploy(container);
    final Set<Container> remaining = subject.clear();
    assertThat(remaining, contains(container));
  }

  @Test
  public void found_schemas_are_a_snapshot() throws Exception {
    final Collection<Container> containers =
        Arrays.<Container>asList(DummySchema.create("1"), DummySchema.create("2"));
    for (Container container : containers) {
      subject.deploy(container);
    }
    final Collection<Container> all = subject.findAll();
    assertThat(all, containsInAnyOrder(containers.toArray()));
    subject.drop(Schema.valueOf("1"));  // catalog changes, but snapshot should not
    assertThat(all, containsInAnyOrder(containers.toArray()));
  }

  // === eventing ==================================================================================

  @Test
  public void emit_event_on_deploying_schema() throws Exception {
    subject.deploy(DummySchema.create("name"));
    assertThat(events.captured(), contains(instanceOf(CatalogEvent.SchemaDeployed.class)));
  }

  @Test
  public void emit_event_on_dropping_schema() throws Exception {
    subject.deploy(DummySchema.create("name"));
    subject.drop(Schema.valueOf("name"));
    assertThat(events.captured(), contains(instanceOf(CatalogEvent.SchemaDeployed.class), instanceOf(CatalogEvent.SchemaDropped.class)));
  }

  @Test
  public void emit_drop_and_deploy_when_replacing() throws Exception {
    subject.deploy(DummySchema.create("first"));
    subject.deploy(DummySchema.create("first"));
    assertThat(events.captured(), contains(instanceOf(CatalogEvent.SchemaDeployed.class), instanceOf(CatalogEvent.SchemaDropped.class), instanceOf(CatalogEvent.SchemaDeployed.class)));
  }

  @Test
  public void no_event_if_schema_not_present_on_drop() throws Exception {
    subject.drop(Schema.valueOf("name"));
    assertThat(events.captured(), is(emptyIterable()));
  }

  @Test
  public void emit_drop_for_all_remaining_on_close() throws Exception {
    subject.deploy(DummySchema.create("one"));
    subject.deploy(DummySchema.create("two"));
    subject.clear();
    assertThat(events.captured(), contains(
        instanceOf(CatalogEvent.SchemaDeployed.class), instanceOf(CatalogEvent.SchemaDeployed.class),
        instanceOf(CatalogEvent.SchemaDropped.class), instanceOf(CatalogEvent.SchemaDropped.class)
    ));
  }
}
