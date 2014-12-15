package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.insight.EventLoggerBridge;
import at.ac.univie.isc.asio.insight.EventStream;
import at.ac.univie.isc.asio.tool.Duration;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Ticker;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.concurrent.*;

@Configuration
public class Config {
  private static final Logger log = LoggerFactory.getLogger(Config.class);

  @Autowired
  private Environment env;

  @Bean
  @org.springframework.context.annotation.Scope(BeanDefinition.SCOPE_PROTOTYPE)
  public ProtocolResource protocolResource(final Command.Factory registry,
                                           final Supplier<EventReporter> scopedEventReporter,
                                           final TimeoutSpec timeout) {
    return new ProtocolResource(registry, timeout, scopedEventReporter);
  }

  @Bean
  public Command.Factory registry(final Supplier<EventReporter> reporter) {
    final boolean allowFederated = true;
    final JenaEngine jenaEngine = JenaEngine.create(emptyModel(), globalTimeout(), allowFederated);
    final Scheduler scheduler = Schedulers.from(workerPool());
    final EngineRegistry registry =
        new EngineRegistry(scheduler, Arrays.<Engine>asList(jenaEngine));
    return new EventfulCommandDecorator(registry, reporter);
  }

  @Bean(destroyMethod = "close")
  public Model emptyModel() {
    return ModelFactory.createDefaultModel();
  }

  @Bean(destroyMethod = "shutdownNow")
  public ExecutorService workerPool() {
    final Integer maxWorkers = env.getProperty("asio.worker.max", Integer.class, 30);
    final ThreadFactory factory =
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("asio-worker-%d")
            .build();
    final BlockingQueue<Runnable> taskQueue = new SynchronousQueue<>();
    return new ThreadPoolExecutor(5, maxWorkers, 0L, TimeUnit.MILLISECONDS, taskQueue, factory);
  }

  @Bean
  public TimeoutSpec globalTimeout() {
    final Long timeout = env.getProperty("asio.timeout", Long.class, -1L);
    final TimeoutSpec spec = TimeoutSpec.from(timeout, TimeUnit.SECONDS);
    log.info(Scope.SYSTEM.marker(), "using timeout {}", spec);
    return spec;
  }

  @Bean
  public Supplier<EventReporter> eventBuilder(final EventBus bus) {
    bus.register(new EventLoggerBridge());
    return Suppliers.ofInstance(new EventReporter(bus, Ticker.systemTicker()));
  }

  @Bean
  public EventStream eventStream(final EventBus bus) {
    final int bufferSize = env.getProperty("asio.event.buffer-size", Integer.class, 50);
    final long windowLength = env.getProperty("asio.event.window-length", Long.class, 10L);
    final EventStream stream =
        new EventStream(Schedulers.io(), Duration.create(windowLength, TimeUnit.SECONDS), bufferSize);
    bus.register(stream);
    return stream;
  }

  @Bean
  public EventBus eventBus() {
    return new EventBus();
  }
}
