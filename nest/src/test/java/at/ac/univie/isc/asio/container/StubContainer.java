package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import rx.Observable;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class StubContainer implements Container {
  public static StubContainer create(final String name) {
    return new StubContainer(Schema.valueOf(name));
  }

  private final Schema schema;
  private Set<Engine> engines = new HashSet<>();

  protected StubContainer(final Schema schema) {
    this.schema = schema;
  }

  @Override
  public Schema name() {
    return schema;
  }

  @Override
  public ContainerSettings settings() {
    return ContainerSettings.of(schema);
  }

  @Override
  public final Set<Engine> engines() {
    return engines;
  }

  @Override
  public Observable<SchemaDescriptor> metadata() {
    return Observable.empty();
  }

  @Override
  public Observable<SqlSchema> definition() {
    return Observable.empty();
  }

  @Nonnull
  public StubContainer withEngine(final Engine engine) {
    engines.add(engine);
    return this;
  }
}
