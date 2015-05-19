/*
 * #%L
 * asio integration
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

import at.ac.univie.isc.asio.Unchecked;
import org.junit.rules.ExternalResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Allows to start a spring application and ensures it is stopped after the test. Also allows to
 * get the server port.
 */
public final class ApplicationRunner extends ExternalResource {
  public static ApplicationRunner run(final Object... sources) {
    return create(new SpringApplicationBuilder(sources));
  }

  public static ApplicationRunner create(final SpringApplicationBuilder builder) {
    return new ApplicationRunner(builder);
  }

  private final SpringApplicationBuilder builder;
  private ApplicationContext context;

  private ApplicationRunner(final SpringApplicationBuilder builder) {
    this.builder = builder;
  }

  public ApplicationRunner profile(final String... profiles) {
    builder.profiles(profiles);
    return this;
  }

  public ApplicationRunner run(final String... args) {
    assert context == null : "application already started";
    context = builder.showBanner(false).run(args);
    Unchecked.sleep(100, TimeUnit.MILLISECONDS);  // brief pause to allow initialization to complete
    System.out.printf(Locale.ENGLISH, ">> running application [%s (%s)]%n", context.getId(), context);
    return this;
  }

  public int getPort() {
    requireNonNull(context, "application not started or startup failed");
    if (context instanceof AnnotationConfigEmbeddedWebApplicationContext) {
      return ((AnnotationConfigEmbeddedWebApplicationContext) context).getEmbeddedServletContainer().getPort();
    } else {
      throw new IllegalStateException("spring application is not a webapp : " + context);
    }
  }

  public <T> T property(final String key, final Class<T> clazz) {
    return context.getEnvironment().getProperty(key, clazz);
  }

  @Override
  protected void after() {
    if (context != null) {
      System.out.printf(Locale.ENGLISH, "<< stopping application [%s (%s)]%n", context.getId(), context);
      SpringApplication.exit(context);
    } else {
      System.err.printf(Locale.ENGLISH, "!! application has not been started - forgot ApplicationRunner#run()?");
    }
  }
}
