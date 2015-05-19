/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.Scope;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pass all beans to the configured {@link com.google.common.eventbus.EventBus} for registration.
 */
public final class EventBusAutoRegistrar implements BeanPostProcessor {
  private static final Logger log = getLogger(EventBusAutoRegistrar.class);

  private final EventBus eventBus;

  public EventBusAutoRegistrar(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
    log.trace(Scope.SYSTEM.marker(), "event bus registration of {}({})", beanName, bean.getClass());
    eventBus.register(bean);
    return bean;
  }

  /** noop */
  @Override
  public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
    return bean;
  }
}
