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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Create spring child application contexts, using the current context as parent. The factory
 * ensures that the context has a unique id and uses a label where appropriate to ease debugging.
 */
@Component
public final class SpringContextFactory {
  private static final AtomicInteger counter = new AtomicInteger(0);

  private final ApplicationContext root;

  @Autowired
  public SpringContextFactory(final ApplicationContext root) {
    this.root = root;
  }

  public AnnotationConfigApplicationContext named(final String label) {
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setParent(root);
    context.setId(root.getId() + ":" + label + ":" + counter.getAndIncrement());
    context.setDisplayName(label);
    return context;
  }

  @Override
  public String toString() {
    return "SpringContextFactory{" + root.getId() + '}';
  }
}
