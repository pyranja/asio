/*
 * #%L
 * asio cli
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
package at.ac.univie.isc.asio.tool;

import com.google.common.io.Resources;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Create layered {@link java.util.Properties} with values defined in one or more sources.
 * - .properties file on the classpath
 * - system properties
 * - external .properties file
 * - command line
 */
public final class PropertyHierarchy {
  private static final Logger log = getLogger(PropertyHierarchy.class);

  /**
   * key of the property pointing to an external .properties file
   */
  public static final String EXTERNAL_LOCATION_PROPERTY = "config_location";

  private Properties merged;

  public PropertyHierarchy() {
    merged = new Properties();
  }

  private static RuntimeException wrapped(final Exception e) {
    throw new IllegalStateException("failed to load property hierarchy - " + e.getMessage(), e);
  }

  /**
   * The merged property hierarchy.
   */
  public Properties get() {
    return merged;
  }

  /**
   * Add all properties from a file in the classpath, e.g. embedded in the .jar.
   */
  public PropertyHierarchy loadEmbedded(final String location) {
    log.debug("loading embedded properties from {}", location);
    final URL resource = Resources.getResource(location);
    try (final InputStream stream = resource.openStream()) {
      merged.load(stream);
    } catch (IOException e) {
      throw wrapped(e);
    }
    return this;
  }

  /**
   * Add all properties from the system environment.
   */
  public PropertyHierarchy loadSystem() {
    log.debug("loading system properties");
    merged.putAll(System.getenv());
    merged.putAll(System.getProperties());
    return this;
  }

  /**
   * Load properties from an external file. If present the {@link #EXTERNAL_LOCATION_PROPERTY} is
   * searched instead of the current directory
   */
  public PropertyHierarchy loadExternal(final String filename) {
    final String externalLocation = merged.getProperty(EXTERNAL_LOCATION_PROPERTY, "");
    final Path path = Paths.get(externalLocation, filename);
    if (Files.isReadable(path)) {
      log.debug("loading external properties from {}", path);
      try (final InputStream stream = Files.newInputStream(path)) {
        merged.load(stream);
      } catch (final IOException e) {
        throw wrapped(e);
      }
    } else {
      log.warn("cannot read properties from {} - check permissions if the file exists", path);
    }
    return this;
  }

  /**
   * Add all non-positional arguments as property, expects --key=value or --toggle format.
   */
  public PropertyHierarchy parseCommandLine(final String[] arguments) {
    log.debug("parsing cli arguments {}", (Object) arguments);
    for (final String arg : arguments) {
      if (arg.startsWith("--")) {
        final String option = arg.substring(2);
        if (!option.isEmpty()) {
          include(option);
        }
      }
    }
    return this;
  }

  private void include(final String arg) {
    final String[] parts = arg.split("=", 2);
    if (parts.length == 1) {  // toggle
      log.debug("found cli toggle {}", parts[0]);
      merged.setProperty(parts[0], "true");
    } else {  // key-value argument
      log.debug("found cli option {} with value {}", parts[0], parts[1]);
      merged.setProperty(parts[0], parts[1]);
    }
  }

  PropertyHierarchy single(final String key, final String value) {
    merged.setProperty(key, value);
    return this;
  }
}
