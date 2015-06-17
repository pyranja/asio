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
package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.tool.JvmOptions;
import at.ac.univie.isc.asio.tool.JvmSystemInfo;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Determine and print recommended system config.
 */
final class Configure implements Command {
  public static final String DEFAULT_LOGPATH = "/var/log/asio";
  public static final String DEFAULT_HEAP_SHARE = "0.7";

  private final Appendable out;
  private final StringBuilder buffer = new StringBuilder();
  private final Settings config;

  Configure(final Appendable out, final Settings config) {
    this.out = out;
    this.config = config;
  }

  @Override
  public String toString() {
    return "print recommended system settings - accepts --relative-heap-size, --log-path";
  }

  @Override
  public int call(final List<String> arguments) throws IOException {
    line("## asio system config // sourced by management scripts ##");
    line();
    printJava();
    line();
    printPaths();
    out.append(buffer);
    return 0;
  }

  private void printJava() throws IOException {
    variable("JAVA", JvmSystemInfo.create().getJvmExecutable().toString());
    variable("JAVA_OPTS", JvmOptions.create()
        .forceIPv4()
        .home(Paths.get(config.get("log-path", DEFAULT_LOGPATH)))
        .relativeHeapSize(Double.parseDouble(config.get("relative-heap-size", DEFAULT_HEAP_SHARE)))
        .toString()
    );
  }

  private void printPaths() throws IOException {
    line("## ! do not change ! ##");
    copy("ASIO_BASE", "ASIO_HOME", "ASIO_OWNER");
  }

  private void line(final String... text) throws IOException {
    for (String each : text) {
      buffer.append(each);
    }
    buffer.append(System.lineSeparator());
  }

  private void copy(final String... key) throws IOException {
    for (String each : key) {
      variable(each, config.require(each));
    }
  }

  private void variable(final String key, final String value) throws IOException {
    buffer.append(key).append("=\"").append(value).append("\"").append(System.lineSeparator());
  }
}
