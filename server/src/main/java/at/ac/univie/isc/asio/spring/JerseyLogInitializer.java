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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Override the java.util.logging default config to enable debug logging in Jersey.
 */
public final class JerseyLogInitializer implements SmartApplicationListener {
  private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 12; // spring boot logging listener is <+ 11>

  private static final String GLOBAL_DEBUG_FLAG = "debug";
  private static final String JERSEY_DEBUG_FLAG = "spring.jersey.debug";

  private static final Level JERSEY_DEFAULT_LEVEL = Level.WARNING;
  private static final Level JERSEY_DEBUG_LEVEL = Level.CONFIG;

  private static final String JERSEY_ROOT_LOGGER =
      "org.glassfish.jersey";
  private static final String JERSEY_CONFIGURATION_LOGGER =
      "org.glassfish.jersey.server.ApplicationHandler";
  private static final String JERSEY_SPRING_LOGGER =
      "org.glassfish.jersey.server.spring.SpringComponentProvider";

  /**
   * Hold strong references to manipulated loggers to avoid losing config due to garbage collection.
   * @see <a href='http://jira.qos.ch/browse/LOGBACK-404'>http://jira.qos.ch/browse/LOGBACK-404</a>
   */
  private final Set<Logger> configuredLoggers = Collections.synchronizedSet(new HashSet<Logger>());

  @Override
  public void onApplicationEvent(final ApplicationEvent event) {
    if (event instanceof ApplicationEnvironmentPreparedEvent) {
      configureJerseyLogging(((ApplicationEnvironmentPreparedEvent) event).getEnvironment());
    }
  }

  private void configureJerseyLogging(final ConfigurableEnvironment environment) {
    enable(JERSEY_ROOT_LOGGER, JERSEY_DEFAULT_LEVEL);
    if (debugModeEnabled(environment)) {
      enable(JERSEY_CONFIGURATION_LOGGER, JERSEY_DEBUG_LEVEL);
      enable(JERSEY_SPRING_LOGGER, JERSEY_DEBUG_LEVEL);
    }
  }

  private boolean debugModeEnabled(final ConfigurableEnvironment environment) {
    return environment.containsProperty(GLOBAL_DEBUG_FLAG)
        || environment.getProperty(JERSEY_DEBUG_FLAG, Boolean.class, false);
  }

  private void enable(final String loggerName, final Level level) {
    final Logger configLogger = Logger.getLogger(loggerName);
    configLogger.setLevel(level);
    configuredLoggers.add(configLogger);
  }

  @Override
  public boolean supportsEventType(final Class<? extends ApplicationEvent> eventType) {
    return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
  }

  @Override
  public boolean supportsSourceType(final Class<?> sourceType) {
    return SpringApplication.class.isAssignableFrom(sourceType);
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
