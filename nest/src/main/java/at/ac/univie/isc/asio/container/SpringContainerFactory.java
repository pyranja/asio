package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicInteger;

import static at.ac.univie.isc.asio.Scope.SYSTEM;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Construct and configure {@link SpringContainer schemas} by instantiating a
 * child {@code ApplicationContext} that holds all schema components.
 */
@Service
final class SpringContainerFactory {
  private static final Logger log = getLogger(SpringContainerFactory.class);

  private final ApplicationContext root;
  private final AtomicInteger counter = new AtomicInteger(0);

  @Autowired
  public SpringContainerFactory(final ApplicationContext root) {
    Assert.isNull(root.getParent(), Pretty.format("non-root context %s injected into SchemaFactory", root));
    this.root = root;
  }

  /**
   * Create a Spring-based container with the given settings.
   *
   * @param settings variable parameters for the container
   * @return initialized and started container
   */
  public SpringContainer createFrom(final ContainerSettings settings) {
    return create(new MapPropertySource("settings", settings.asMap()));
  }

  /**
   * Use the given property source to initialize a
   * {@link at.ac.univie.isc.asio.container.SpringBluePrint physical schema}.
   *
   * @param properties settings as defined in {@link ContainerSettings}
   * @return created schema
   */
  public SpringContainer create(final PropertySource<?> properties) {
    final String prefix = "schema-" + counter.getAndIncrement();
    log.debug(SYSTEM.marker(), "creating {} from {}", prefix, properties);
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setParent(root);
    context.getEnvironment().getPropertySources().addFirst(properties); // given properties are prime source of settings
    context.register(SpringBluePrint.class);
    context.setBeanNameGenerator(new PrefixingBeanNameGenerator(prefix));
    context.setDisplayName(prefix);
    context.setId(prefix);
    context.refresh();
    log.debug(SYSTEM.marker(), "created {}", context);
    return SpringContainer.create(context);
  }

  /**
   * Prefix all bean names with a fixed string, to ease debugging.
   */
  private static class PrefixingBeanNameGenerator implements BeanNameGenerator {
    private final BeanNameGenerator delegate = new AnnotationBeanNameGenerator();
    private final String prefix;

    private PrefixingBeanNameGenerator(final String prefix) {
      this.prefix = prefix;
    }

    @Override
    public String generateBeanName(final BeanDefinition definition, final BeanDefinitionRegistry registry) {
      return prefix + delegate.generateBeanName(definition, registry);
    }
  }
}
