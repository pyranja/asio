package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.springframework.context.ConfigurableApplicationContext;
import rx.Observable;

import java.util.Map;
import java.util.Set;

/**
 * Schema container wrapping a spring {@code ApplicationContext} holding the schema components.
 * This schema should be {@link AutoCloseable#close() closed}, to release the resources in the
 * wrapped {@code ApplicationContext}.
 */
@AutoValue
abstract class SpringContainer implements Container, AutoCloseable {
  SpringContainer() { /* prevent external subclasses */ }

  /**
   * Destroy this schema and release all contained resources.
   */
  @Override
  public void close() {
    context().close();
  }

  /**
   * The {@code ApplicationContext}, that holds all components of this schema.
   * Only for internal use.
   * @return the backing {@code ApplicationContext}.
   */
  abstract ConfigurableApplicationContext context();

  /**
   * @return schema configuration properties
   */
  public abstract ContainerSettings settings();

  /**
   * @return sequence of metadata descriptors
   */
  @Override
  public abstract Observable<SchemaDescriptor> metadata();

  /**
   * @return sequence of schema definitions
   */
  @Override
  public abstract Observable<SqlSchema> definition();

  // implement schema by delegating to stored settings and context

  @Override
  public final Schema name() {
    return settings().getName();
  }

  @Override
  public final Set<Engine> engines() {
    final Map<String, Engine> found = context().getBeansOfType(Engine.class);
    return ImmutableSet.copyOf(found.values());
  }

  @Override
  public String toString() {
    return "SpringContainer{" + context().getDisplayName() + '}';
  }
}
