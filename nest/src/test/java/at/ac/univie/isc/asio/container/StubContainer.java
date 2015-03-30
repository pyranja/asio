package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import rx.Observable;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class StubContainer implements Container {
  public static StubContainer create(final String name) {
    return new StubContainer(Id.valueOf(name));
  }

  private final Id id;
  private boolean closed = false;
  private boolean activated = false;
  private Set<Engine> engines = new HashSet<>();

  protected StubContainer(final Id id) {
    this.id = id;
  }

  @Override
  public Id name() {
    return id;
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

  @Override
  public void activate() throws IllegalStateException {
    activated = true;
  }

  @Override
  public void close() {
    closed = true;
  }

  // === test getter ===============================================================================

  public boolean isClosed() {
    return closed;
  }

  public boolean isActivated() {
    return activated;
  }

  public boolean isRunning() {
    return activated && !closed;
  }
}
