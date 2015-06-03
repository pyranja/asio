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

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Provide methods to gather information about the running JVM. Non-final to allow mocking.
 */
public class JvmSystemInfo {
  /**
   * One MiB is 2^20 bytes
   */
  public static final int MEBI_BYTE = 1024 * 1024;

  public static JvmSystemInfo create() {
    return new JvmSystemInfo();
  }

  private final int jvmSpecVersion;
  private final OperatingSystemMXBean osBean;
  private final Path jvmExecutable;

  private JvmSystemInfo() {
    jvmSpecVersion = detectJvmMinorVersion();
    osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    jvmExecutable = Paths.get(System.getProperty("java.home"), "bin", "java");
  }

  public int getJvmSpecVersion() {
    return jvmSpecVersion;
  }

  public Path getJvmExecutable() {
    return jvmExecutable;
  }

  public long getTotalMemory() {
    return osBean.getTotalPhysicalMemorySize();
  }

  public long getFreeMemory() {
    return osBean.getFreePhysicalMemorySize();
  }

  private int detectJvmMinorVersion() {
    final String versionText = System.getProperty("java.specification.version");
    try {
      return Integer.parseInt(versionText.split(Pattern.quote("."))[1]);
    } catch (final Exception e) {
      throw new AssertionError("cannot detect jvm minor version from " + versionText);
    }
  }

  @Override
  public String toString() {
    return "JvmSystemInfo{" +
        "specVersion=" + jvmSpecVersion +
        ", executable=" + jvmExecutable +
        ", memoryUsage=" + getFreeMemory() / MEBI_BYTE + "MiB/" + getTotalMemory() / MEBI_BYTE
        + "MiB" +
        '}';
  }

  /**
   * print jvm info to the console
   */
  public static void main(String[] args) {
    System.out.println(create());
    System.exit(0);
  }
}
