package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class DummySchema implements Container {
  public static DummySchema create(final String name) {
    return new DummySchema(Schema.valueOf(name));
  }

  private final Schema schema;
  private String identifier;
  private Set<Engine> engines = new HashSet<>();
  private RelationalSchemaService schemaService;

  protected DummySchema(final Schema schema) {
    this.schema = schema;
  }

  @Override
  public Schema name() {
    return schema;
  }

  @Override
  public final String identifier() {
    return identifier;
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
  public DummySchema withIdentifier(final String identifier) {
    this.identifier = identifier;
    return this;
  }

  @Nonnull
  public DummySchema withEngine(final Engine engine) {
    engines.add(engine);
    return this;
  }

  @Nonnull
  public DummySchema withSchemaService(final RelationalSchemaService dataSource) {
    this.schemaService = dataSource;
    return this;
  }
}
