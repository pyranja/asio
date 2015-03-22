package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;
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
  private RelationalSchemaService schemaService;

  protected StubContainer(final Schema schema) {
    this.schema = schema;
  }

  @Override
  public Schema name() {
    return schema;
  }

  @Override
  public Observable<SchemaDescriptor> metadata() {
    return Observable.empty();
  }

  @Override
  public final Set<Engine> engines() {
    return engines;
  }

  @Override
  public final RelationalSchemaService schemaService() {
    return schemaService;
  }

  @Nonnull
  public StubContainer withEngine(final Engine engine) {
    engines.add(engine);
    return this;
  }

  @Nonnull
  public StubContainer withSchemaService(final RelationalSchemaService dataSource) {
    this.schemaService = dataSource;
    return this;
  }
}
