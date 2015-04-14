package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import rx.Observable;

import java.util.Map;
import java.util.Set;

/**
 * Wrap an {@code ApplicationContext}, which holds the components of a dataset.
 * This container <strong>must</strong> be {@link #close() closed} to release the spring context
 * and associated resources.
 */
@AutoValue
abstract class NestContainer implements Container, AutoCloseable {
  NestContainer() { /* prevent subclassing */ }

  public static NestContainer wrap(final ConfigurableApplicationContext context, final NestConfig config) {
    return new AutoValue_NestContainer(context, config, config.getDataset().getName());
  }

  /**
   * The wrapped spring context.
   */
  abstract ConfigurableApplicationContext context();

  // === bind container lifecycle to context lifecycle =============================================

  /**
   * Refresh the wrapped spring context. All component beans are created now.
   */
  @Override
  public void activate() {
    context().refresh();
  }

  /**
   * Close the wrapped spring context and release resources.
   */
  @Override
  public void close() {
    context().close();
  }

  // === container info ============================================================================

  @JsonProperty
  @JsonUnwrapped
  public abstract NestConfig configuration();

  // === component getters delegate to wrapped context =============================================

  /**
   * Name of the container, equal to the {@link ApplicationContext#getDisplayName()}.
   */
  @Override
  public abstract Id name();

  /**
   * Retrieve all beans that implement {@link Engine}.
   */
  @Override
  public final Set<Engine> engines() {
    final Map<String, Engine> found = context().getBeansOfType(Engine.class);
    return ImmutableSet.copyOf(found.values());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Observable<SqlSchema> definition() {
    return context().getBean(NestBluePrint.BEAN_DEFINITION_SOURCE, Observable.class);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Observable<SchemaDescriptor> metadata() {
    return context().getBean(NestBluePrint.BEAN_DESCRIPTOR_SOURCE, Observable.class);
  }

  @Override
  public final String toString() {
    return "NestContainer{" + name() + " | " + configuration() + "}";
  }
}
