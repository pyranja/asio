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
package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.ConfigStore;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.flock.FlockAssembler;
import at.ac.univie.isc.asio.nest.D2rqNestAssembler;
import at.ac.univie.isc.asio.tool.Closer;
import at.ac.univie.isc.asio.tool.StatefulMonitor;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Orchestrate container creation and destruction. On application start all stored configuration
 * items are scanned for containers, that need to be deployed. On shutdown all deployed containers
 * are dropped (but not disposed).
 */
@Component
class Warden implements SmartLifecycle {
  private static final Logger log = getLogger(Warden.class);

  /**
   * identifier of configuration files originating from this warden
   */
  static final String D2RQ_SUFFIX = "config";
  public static final String JSON_SUFFIX = "json";

  private final Catalog catalog;
  private final Assembler d2rqAssembler;
  private final FlockAssembler jsonAssembler;
  private final ConfigStore config;
  private final StatefulMonitor monitor;

  @Autowired
  Warden(final Catalog catalog, final D2rqNestAssembler d2rqAssembler, final FlockAssembler jsonAssembler, final ConfigStore config, final Timeout timeout) {
    log.info(Scope.SYSTEM.marker(), "warden loaded, config-store={}, json-assembler={}, d2rq-assembler={}", config, jsonAssembler, d2rqAssembler);
    this.catalog = catalog;
    this.config = config;
    this.d2rqAssembler = d2rqAssembler;
    this.jsonAssembler = jsonAssembler;
    monitor = StatefulMonitor.withMaximalWaitingTime(timeout);
  }

  @Override
  public String toString() {
    return "Warden{" +
        "config=" + config +
        ", d2rqAssembler=" + d2rqAssembler +
        ", jsonAssembler=" + jsonAssembler +
        '}';
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public int getPhase() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public boolean isRunning() {
    return monitor.isActive();
  }

  // === internal api


  void deployFromD2rqMapping(final Id target, final ByteSource source) {
    assembleAndDeploy(target, source, d2rqAssembler, D2RQ_SUFFIX);
  }

  void deployFromJson(final Id target, final ByteSource source) {
    assembleAndDeploy(target, source, jsonAssembler, JSON_SUFFIX);
  }

  /**
   * Assemble a container from the given configuration data and deploy it with the given id.
   *
   * @param target  id of the new container
   * @param source  raw configuration data of the deployed container
   * @param factory Assembler compatible with the given source
   * @param format  identifier of config format
   */
  private void assembleAndDeploy(final Id target, final ByteSource source, final Assembler factory, final String format) {
    log.debug(Scope.SYSTEM.marker(), "creating <{}> from '{}' sources using {}", target, format, factory.getClass());
    monitor.ensureActive(); // fail fast before doing a costly assembly
    final Container container = factory.assemble(target, source);
    monitor.atomic(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        dispose(target);
        final URI location = config.save(target.asString(), format, source);
        log.debug(Scope.SYSTEM.marker(), "saved configuration at <{}>", location);
        container.activate();
        log.debug(Scope.SYSTEM.marker(), "activated {} as <{}>", container, target);
        final Optional<Container> replaced = catalog.deploy(container);
        cleanUpIfNecessary(replaced);
        assert !replaced.isPresent() : "container was present on deploying of " + target;
        return null;
      }
    });
  }

  /**
   * If present undeploy and dispose the container with given name.
   *
   * @param target name of target container
   * @return true if the target container was present and has been dropped, false if not present
   */
  boolean dispose(final Id target) {
    log.debug(Scope.SYSTEM.marker(), "dispose container <{}>", target);
    return monitor.atomic(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final Optional<Container> dropped = catalog.drop(target);
        cleanUpIfNecessary(dropped);
        config.clear(target.asString());
        return dropped.isPresent();
      }
    });
  }

  // === lifecycle implementation ==================================================================

  /**
   * Start up this Warden. Each configuration item find in the backing
   * {@link ConfigStore persistent store} is read, a container assembled from it and deployed to
   * the {@link Catalog}.
   *
   * @throws IllegalArgumentException if the warden is already running
   */
  @Override
  public void start() {
    log.info(Scope.SYSTEM.marker(), "starting", this);
    monitor.activate(new StatefulMonitor.Action() {
      @Override
      public void run() throws Exception {
        final Map<String, ByteSource> d2rqMappings = config.findAllWithIdentifier(D2RQ_SUFFIX);
        deployBatch(d2rqMappings, d2rqAssembler);
        final Map<String, ByteSource> jsonMappings = config.findAllWithIdentifier(JSON_SUFFIX);
        deployBatch(jsonMappings, jsonAssembler);
      }
    });
  }

  /**
   * Stop this Warden. All currently deployed containers are cleared from the {@link Catalog} and
   * closed. As long as this is stopped, no containers may be
   * {@link #deployFromD2rqMapping(Id, ByteSource) deployed} or {@link #dispose(Id) disposed}.
   *
   * @throws IllegalMonitorStateException if the warden is not running
   */
  @Override
  public void stop() {
    log.info(Scope.SYSTEM.marker(), "stopping");
    monitor.disable(new StatefulMonitor.Action() {
      @Override
      public void run() throws Exception {
        for (final Container container : catalog.clear()) {
          log.debug(Scope.SYSTEM.marker(), "closing {} on stop", container.name());
          Closer.quietly(container);
        }
      }
    });
  }

  /**
   * Stop the Warden asynchronously, same as calling {@link #stop()}, then run the supplied
   * callback.
   */
  @Override
  public void stop(final Runnable callback) {
    stop();
    callback.run();
  }

  /**
   * Deploy all given id->configurations mappings as container, using the given assembler type.
   */
  private void deployBatch(final Map<String, ByteSource> found, final Assembler factory) {
    log.debug(Scope.SYSTEM.marker(), "found configurations of {}", found.keySet());
    for (Map.Entry<String, ByteSource> current : found.entrySet()) {
      final Id name = Id.valueOf(current.getKey());
      log.info(Scope.SYSTEM.marker(), "deploying <{}> on startup", name);
      final Optional<Container> replaced = deployQuietly(name, current.getValue(), factory);
      cleanUpIfNecessary(replaced);
    }
  }

  /**
   * Attempt to assemble, activate and deploy a container with given name and config,
   * but suppress any errors. Return any replaced, if one was present.
   */
  private Optional<Container> deployQuietly(final Id name, final ByteSource config, final Assembler factory) {
    try {
      final Container container = factory.assemble(name, config);
      container.activate();
      return catalog.deploy(container);
    } catch (final Exception cause) {
      log.error(Scope.SYSTEM.marker(), "quiet deployment of a container <{}> failed", name, cause);
      return Optional.absent();
    }
  }

  /**
   * if present close the given container
   */
  private void cleanUpIfNecessary(final Optional<Container> dropped) {
    if (dropped.isPresent()) {
      final Container container = dropped.get();
      log.debug(Scope.SYSTEM.marker(), "cleaning up {}", container);
      Closer.quietly(container);
    }
  }
}
