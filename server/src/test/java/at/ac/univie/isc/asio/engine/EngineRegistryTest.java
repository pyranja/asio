package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.container.CatalogEvent;
import at.ac.univie.isc.asio.container.Container;
import at.ac.univie.isc.asio.container.StubContainer;
import at.ac.univie.isc.asio.security.Identity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public class EngineRegistryTest {
  @Rule
  public ExpectedException error = ExpectedException.none();
  private final EngineRegistry subject = new EngineRegistry();

  @Test
  public void should_fail_if_schema_not_found() throws Exception {
    error.expect(Id.NotFound.class);
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
  }

  @Test
  public void should_fail_if_language_not_supported() throws Exception {
    subject.onDeploy(new CatalogEvent.SchemaDeployed(StubContainer.create("default")));
    error.expect(Language.NotSupported.class);
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
  }

  @Test
  public void should_find_engine_identified_by_schema_and_language() throws Exception {
    final Engine expected = new StubEngine();
    final Container container = StubContainer.create("default").withEngine(expected);
    subject.onDeploy(new CatalogEvent.SchemaDeployed(container));
    final Engine selected = subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    assertThat(selected, sameInstance(expected));
  }

  @Test
  public void should_forget_undeployed_schemas() throws Exception {
    final Container container = StubContainer.create("default").withEngine(new StubEngine());
    subject.onDeploy(new CatalogEvent.SchemaDeployed(container));
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    subject.onDrop(new CatalogEvent.SchemaDropped(container));
    error.expect(Id.NotFound.class);
    subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
  }

  @Test
  public void should_replace_if_deployed_already_present() throws Exception {
    final Engine first = new StubEngine();
    final Engine second = new StubEngine();
    Engine selected;
    subject.onDeploy(new CatalogEvent.SchemaDeployed(StubContainer.create("default").withEngine(first)));
    selected = subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    assertThat(selected, sameInstance(first));
    subject.onDeploy(new CatalogEvent.SchemaDeployed(StubContainer.create("default").withEngine(second)));
    selected = subject.select(command(Id.valueOf("default"), Language.UNKNOWN));
    assertThat(selected, sameInstance(second));
  }

  private Command command(final Id id, final Language language) {
    return CommandBuilder.empty().language(language)
        .single(Command.KEY_SCHEMA, id.asString())
        .owner(Identity.undefined())
        .build();
  }

  private static class StubEngine implements Engine {
    @Override
    public Language language() {
      return Language.UNKNOWN;
    }

    @Override
    public Invocation prepare(final Command command) {
      throw new UnsupportedOperationException("fake");
    }

    @Override
    public void close() throws DatasetException {

    }
  }
}
