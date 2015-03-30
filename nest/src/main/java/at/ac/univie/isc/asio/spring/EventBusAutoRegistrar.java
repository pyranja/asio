package at.ac.univie.isc.asio.spring;

import com.google.common.eventbus.EventBus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * Pass all beans to the configured {@link com.google.common.eventbus.EventBus} for registration.
 */
@Component
public final class EventBusAutoRegistrar implements BeanPostProcessor {
  private final EventBus eventBus;

  @Autowired
  public EventBusAutoRegistrar(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
    eventBus.register(bean);
    return bean;
  }

  /** noop */
  @Override
  public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
    return bean;
  }
}
