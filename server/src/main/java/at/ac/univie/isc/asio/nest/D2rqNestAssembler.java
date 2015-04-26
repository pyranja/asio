package at.ac.univie.isc.asio.nest;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.brood.Assembler;
import at.ac.univie.isc.asio.d2rq.D2rqConfigModel;
import at.ac.univie.isc.asio.d2rq.D2rqJdbcModel;
import at.ac.univie.isc.asio.d2rq.LoadD2rqModel;
import at.ac.univie.isc.asio.database.Jdbc;
import at.ac.univie.isc.asio.spring.SpringContextFactory;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;

import javax.annotation.Nonnull;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Create container based on the {@link at.ac.univie.isc.asio.nest.NestBluePrint}.
 */
@Brood
final class D2rqNestAssembler implements Assembler {
  private static final Logger log = getLogger(D2rqNestAssembler.class);

  private final SpringContextFactory create;
  private final List<Configurer> configurers;
  private final List<OnClose> cleaners;

  /** Ensure there is always at least on Configurer and OnClose available. */
  @Bean
  public static ListenerDummy dummyConfigurer() {
    return new ListenerDummy();
  }

  @Autowired
  public D2rqNestAssembler(final SpringContextFactory create,
                           final List<Configurer> configurers,
                           final List<OnClose> cleaners) {
    this.create = create;
    this.configurers = configurers;
    this.cleaners = cleaners;
  }

  @Override
  public String toString() {
    return "D2rqNestAssembler{" +
        "create=" + create +
        ", configurers=" + configurers +
        '}';
  }

  @Override
  public Container assemble(final Id name, final ByteSource source) {
    log.debug(Scope.SYSTEM.marker(), "assemble <{}> from {}", name, source);
    final Model model = LoadD2rqModel.inferBaseUri().parse(source);
    final NestConfig initial = parse(model);
    initial.getDataset().setName(name);
    log.debug(Scope.SYSTEM.marker(), "initial config for {} : {}", name, initial);
    final NestConfig processed = postProcess(initial, configurers);
    log.debug(Scope.SYSTEM.marker(), "final config for {} : {}", name, initial);
    final AnnotationConfigApplicationContext context = create.named(name.asString());
    inject(context, processed);
    log.debug(Scope.SYSTEM.marker(), "assembled container <{}> with {} ({})", name, context.getId(), context);
    return NestContainer.wrap(context, processed);
  }

  // === assembly steps - package visible for testing ==============================================

  final NestConfig parse(final Model model) {
    final D2rqConfigModel d2rq = D2rqConfigModel.wrap(model);
    final Dataset dataset = new Dataset()
        .setIdentifier(d2rq.getIdentifier())
        .setTimeout(d2rq.getTimeout())
        .setFederationEnabled(d2rq.isFederationEnabled());
    final D2rqJdbcModel jdbcConfig = d2rq.getJdbcConfig();
    final Jdbc jdbc = new Jdbc()
        .setUrl(jdbcConfig.getUrl())
        .setSchema(jdbcConfig.getSchema())
        .setDriver(jdbcConfig.getDriver())
        .setUsername(jdbcConfig.getUsername())
        .setPassword(jdbcConfig.getPassword())
        .setProperties(jdbcConfig.getProperties());
    return NestConfig.create(dataset, jdbc, d2rq);
  }

  final NestConfig postProcess(final NestConfig initial, final List<Configurer> configurers) {
    assert initial.getDataset().getName() != null : "implementation error - dataset name not set";
    NestConfig processed = initial;
    for (Configurer configurer : configurers) {
      log.debug(Scope.SYSTEM.marker(), "applying {} to {}", configurer, processed);
      processed = configurer.apply(processed);
    }
    return processed;
  }

  final void inject(final AnnotationConfigApplicationContext context, final NestConfig config) {
    context.register(NestBluePrint.class);
    final ConfigurableListableBeanFactory beans = context.getBeanFactory();
    beans.registerSingleton("container-cleanup", new ContainerCleanUp(context, config, cleaners));
    beans.registerSingleton("dataset", config.getDataset());
    beans.registerSingleton("jdbc", config.getJdbc());
    beans.registerSingleton("d2rq", config.getD2rq());
  }

  /** attach OnClose listeners to a container */
  static final class ContainerCleanUp implements ApplicationListener<ContextClosedEvent> {
    private final ApplicationContext target;
    private final NestConfig config;
    private final List<OnClose> actions;

    ContainerCleanUp(final ApplicationContext target, final NestConfig config, final List<OnClose> actions) {
      this.target = target;
      this.config = config;
      this.actions = actions;
    }

    @Override
    public void onApplicationEvent(final ContextClosedEvent event) {
      if (event.getSource() == target) {
        log.info(Scope.SYSTEM.marker(), "cleaning up destroyed container using {}", actions);
        for (final OnClose listener : actions) {
          try {
            listener.cleanUp(config);
          } catch (final RuntimeException e) {
            log.info(Scope.SYSTEM.marker(), "error during container clean up", e);
          }
        }
      } else {
        log.debug(Scope.SYSTEM.marker(), "ignoring destruction of {}", event.getSource());
      }
    }
  }

  /** used to prevent wiring failure if no Configurer or OnClose present */
  static final class ListenerDummy implements Configurer, OnClose {
    @Nonnull
    @Override
    public NestConfig apply(final NestConfig input) {
      return input;
    }

    @Override
    public void cleanUp(final NestConfig spec) throws RuntimeException {}

    @Override
    public String toString() {
      return "Dummy{}";
    }
  }
}
