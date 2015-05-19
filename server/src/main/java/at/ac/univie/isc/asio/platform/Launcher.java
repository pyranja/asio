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
package at.ac.univie.isc.asio.platform;

import at.ac.univie.isc.asio.AsioError;
import at.ac.univie.isc.asio.tool.Pretty;
import com.sun.akuma.CLibrary;
import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;

import java.io.IOException;
import java.util.Locale;

/**
 * Fork into a child process and detach from the terminal to enable daemon mode for an application.
 * Launching can be controlled by providing system properties:
 * <ul>
 * <li>{@code daemon}   : if missing or value is not {@link Boolean#TRUE true}, forking is skipped</li>
 * <li>{@code app-id}   : name of the application, used for logging (default: daemon)</li>
 * <li>{@code pidfile}  : path of the pid file - if missing, pid file creation is skipped</li>
 * </ul>
 *
 * @see com.sun.akuma.Daemon
 */
public final class Launcher {
  /**
   * Create a Launcher, configured from system properties.
   */
  public static Launcher currentProcess() {
    final String workDirectory = System.getProperty("asio.home", System.getProperty("java.io.tmpdir"));
    return new Launcher(new ChangeToHomeDaemon(workDirectory), new Companion());
  }

  private final Daemon daemon;
  private final Companion companion;

  Launcher(final Daemon daemon, final Companion companion) {
    this.daemon = daemon;
    this.companion = companion;
  }

  /**
   * Fork and exec the current process if daemon mode is enabled and the app was just started.
   * If in a forked process, detach from process group and close in/out channels.
   *
   * @throws at.ac.univie.isc.asio.platform.Launcher.StartupFailed on any error
   */
  public void daemonize() {
    try {
      if (daemon.isDaemonized()) {
        final String pidfile = companion.pidfile();
        companion.log("detaching daemon process (pid file : %s)", pidfile);
        daemon.init(pidfile);
      } else if (companion.shouldDaemonize()) {
        final JavaVMArguments args = companion.arguments();
        companion.log("forking daemon process (cli args : %s)", args);
        daemon.daemonize(args);
        companion.log("daemon forked - exiting parent");
        companion.exit();
      } else {
        companion.log("daemon not enabled");
      }
    } catch (Exception e) {
      companion.log("failure - %s", e);
      throw new StartupFailed(e);
    }
  }

  /**
   * Thrown if startup as daemon fails.
   */
  public static final class StartupFailed extends AsioError.Base {
    protected StartupFailed(final Throwable cause) {
      super("forking daemon failed", cause);
    }
  }

  /** Encapsulate hard to mock system calls to ease testing */
  static class Companion {
    boolean shouldDaemonize() {
      return Boolean.valueOf(System.getProperty("daemon", "false"));
    }

    String pidfile() {
      return System.getProperty("pidfile");
    }

    String id() {
      return System.getProperty("app-id", "daemon");
    }

    void exit() {
      System.exit(0);
    }

    JavaVMArguments arguments() throws IOException {
      return JavaVMArguments.current();
    }

    void log(final String template, final Object... args) {
      final String message = Pretty.format(template, args);
      System.out.printf(Locale.ENGLISH, "[%s/daemon] %s%n", id(), message);
    }
  }

  /** daemon specialization that will change directory to a fixed one */
  private static class ChangeToHomeDaemon extends Daemon {
    private final String workDirectory;

    private ChangeToHomeDaemon(final String workDirectory) {
      this.workDirectory = workDirectory;
    }

    @Override
    protected void chdirToRoot() {
      CLibrary.LIBC.chdir(workDirectory);
      System.setProperty("user.dir", workDirectory);
    }
  }
}
