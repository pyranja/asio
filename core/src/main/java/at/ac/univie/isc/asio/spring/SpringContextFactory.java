package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.Scope;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Create spring child application contexts, using the current context as parent. The factory
 * ensures that the context has a unique id and uses a label where appropriate to ease debugging.
 */
@Component
public final class SpringContextFactory {
  private static final Logger log = getLogger(SpringContextFactory.class);

  private static final AtomicInteger counter = new AtomicInteger(0);

  private final ApplicationContext root;

  @Autowired
  public SpringContextFactory(final ApplicationContext root) {
    log.info(Scope.SYSTEM.marker(), "using {} ({}) as parent context", root.getId(), root);
    this.root = root;
  }

  public AnnotationConfigApplicationContext named(final String label) {
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setParent(root);
    context.setId(root.getId() + ":" + label + ":" + counter.getAndIncrement());
    context.setDisplayName(label);
    return context;
  }
}
