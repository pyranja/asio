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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import org.slf4j.Logger;

import java.io.File;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Build dynamic JVM startup parameters, taking into account information from the current runtime.
 */
public final class JvmOptions {
  private static final Logger log = getLogger(JvmOptions.class);

  public static final int MEBI_BYTE = 1024 * 1024;

  private static final String MODE = "-server";
  private static final String DNS_CACHE_TTL = "-Dsun.net.inetaddr.ttl=60";
  private static final String[] FORCE_IPV4 =
      {"-Djava.net.preferIPv4Stack=true", "-Djava.net.preferIPv6Addresses=false"};

  private final JvmSystemInfo system = JvmSystemInfo.create();

  // toggles
  private boolean forceIPv4 = false;
  private double relativeHeapSize = -1.0;
  private String home = asDirectoryPath(Paths.get(System.getProperty("java.io.tmpdir")));

  public static JvmOptions create() {
    return new JvmOptions();
  }

  private JvmOptions() {
  }

  /**
   * Inspect environment and determine JVM options for current settings.
   *
   * @return a collection of JVM startup arguments
   */
  public Collection<String> collect() {
    log.debug("using <{}> as home directory", home);
    final ImmutableList.Builder<String> args = ImmutableList.builder();
    args.add(MODE, DNS_CACHE_TTL);
    args.add(logging());
    if (forceIPv4) {
      args.add(FORCE_IPV4);
    }
    args.add(garbageCollector());
    args.add(heap());
    return args.build();
  }

  private static final Joiner WHITESPACE_JOINER =Joiner.on(' ');

  /**
   * @return the collected jvm arguments as a command line string (separated by a single space)
   */
  @Override
  public String toString() {
    return WHITESPACE_JOINER.join(collect());
  }

  /**
   * turn a raw path text into a folder path, with platform dependent trailing separator
   */
  private String asDirectoryPath(final Path path) {
    String result = path.normalize().toAbsolutePath().toString();
    if (!result.endsWith(File.separator)) {
      result += File.separator;
    }
    return result;
  }

  private String[] logging() {
    return new String[] {
        // heap logging
        "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=" + home,
        // gc logging
        "-verbose:gc", "-XX:+PrintGCDetails", "-XX:+PrintGCDateStamps",
        "-Xloggc:" + home + "gc.log", "-XX:+UseGCLogFileRotation", "-XX:NumberOfGCLogFiles=4", "-XX:GCLogFileSize=10M"
    };
  }

  private String[] garbageCollector() {
    return new String[] {
        "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=250", "-XX:+ScavengeBeforeFullGC"
    };
  }

  private String[] heap() {
    if (relativeHeapSize < 0) {
      return new String[0];
    }
    final long total = system.getTotalMemory() / MEBI_BYTE;
    final long heap = DoubleMath.roundToLong(total * relativeHeapSize, RoundingMode.UP);
    final long free = system.getFreeMemory() / MEBI_BYTE;
    log.debug("using {}MiB as heap size (~{}% of total {}Mib)", heap, relativeHeapSize * 100, total);
    if (heap > free) {
      log.error("calculated heap size of {}MiB is greater than current free amount of memory ({}MiB)", heap, free);
    }
    final String metaSpaceArg = system.getJvmSpecVersion() > 7
        ? "-XX:MaxMetaspaceSize=" + DoubleMath.roundToLong(total * 0.5, RoundingMode.CEILING)
        : "-XX:MaxPermSize=" + Math.max(128, DoubleMath.roundToLong(total * 0.05, RoundingMode.CEILING));
    return new String[] {
        "-Xms" + heap + "M", "-Xmx" + heap + "M", metaSpaceArg
    };
  }

  // toggles

  /**
   * Force the JVM to use the IPv4 stack, instead of favoring IPv6
   */
  public JvmOptions forceIPv4() {
    forceIPv4 = true;
    return this;
  }

  public JvmOptions home(final Path folder) {
    home = asDirectoryPath(folder);
    return this;
  }

  public JvmOptions relativeHeapSize(final double percentage) {
    Preconditions.checkArgument(percentage > 0.0 && percentage < 1.0, "illegal relative heap size " + percentage);
    this.relativeHeapSize = percentage;
    return this;
  }
}
