package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.engine.Engine;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;
import java.util.Set;

/**
 * Schema container wrapping a spring {@code ApplicationContext} holding the schema components.
 */
@AutoValue
public abstract class SpringSchemaContainer implements Schema {
  /**
   * Create a schema using components from the given {@code ApplicationContext}.
   *
   * @param context schema configuration
   * @return schema facade
   */
  static Schema create(final ConfigurableApplicationContext context) {
    final Map<String, Engine> engines = context.getBeansOfType(Engine.class);
    final PhysicalSchemaSettings settings = context.getBean(PhysicalSchemaSettings.class);
    return new AutoValue_SpringSchemaContainer(context, settings, ImmutableSet.copyOf(engines.values()));
  }

  SpringSchemaContainer() { /* prevent external subclasses */ }

  /**
   * The {@code ApplicationContext}, that holds all components of this schema.
   * Only for internal use.
   * @return the backing {@code ApplicationContext}.
   */
  abstract ConfigurableApplicationContext context();

  /**
   * @return schema configuration properties
   */
  abstract PhysicalSchemaSettings settings();

  /**
   * All configured engines for this schema, i.e. sql and sparql.
   *
   * @return set of sql and sparql engine
   */
  public abstract Set<Engine> engines();

  /**
   * Local name of this schema. Equal to the mysql schema name.
   *
   * @return the local name of this schema
   */
  @Override
  public final String name() {
    return settings().getName();
  }

  /**
   * Global name of this schema, e.g. as used in a metadata repository.
   *
   * @return the global identifier of this schema
   */
  @Override
  public String identifier() {
    return settings().getIdentifier();
  }

  /**
   * Destroy this schema and release all contained resources.
   */
  @Override
  public void close() {
    context().close();
  }

  @Override
  public String toString() {
    return "Schema{" + context().getDisplayName() + '}';
  }
}
