package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.tool.Pretty;
import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;

import java.util.Locale;

import static com.sun.akuma.CLibrary.LIBC;

/**
 * Run a java application in a background process. Daemonization is controlled by system properties:
 * <ul>
 *   <li>{@code asio.daemon}    : true if process should be run as a daemon</li>
 *   <li>{@code asio.daemon.id} : used as pid file name ({@code /var/run/asio/${asio.daemon.id}.pid })</li>
 *   <li>{@code asio.base}      : asio installation directory</li>
 * </ul>
 *
 * @see {@link com.sun.akuma.Daemon}
 */
public final class Daemonize extends Daemon {
  /**
   * @return new Daemonize instance for current process
   */
  public static Daemonize current() {
    return new Daemonize();
  }

  private Daemonize() {}

  /**
   * Fork and exec the current process if daemon mode is enabled and the app was just started.
   * If in a forked process, detach from process group and close in/out channels.
   *
   * @throws at.ac.univie.isc.asio.Daemonize.DaemonizeFailed on any error
   */
  public final void process() throws DaemonizeFailed {
    try {
      if (isDaemonized()) {
        final String pidFile = Pretty.format("/var/run/asio/%s.pid", applicationId());
        log("detaching daemon process (pid file : %s)", pidFile);
        init(pidFile);
      } else if (shouldDaemonize()) {
        final JavaVMArguments args = JavaVMArguments.current();
        log("forking daemon process (cli args : %s)", args);
        daemonize(args);
        log("daemon forked - exiting parent");
        System.exit(0);
      } else {
        log("skip daemonizing");
      }
    } catch (Exception e) {
      log("failure - %s", e);
      throw new DaemonizeFailed(e);
    }
  }

  private boolean shouldDaemonize() {
    return Boolean.valueOf(System.getProperty("asio.daemon", "false"));
  }

  private String applicationId() {
    return System.getProperty("asio.daemon.id", "daemon");
  }

  private String workDirectory() {
    return System.getProperty("asio.home", System.getProperty("asio.base", System.getProperty("java.io.tmpdir")));
  }

  @Override
  protected void chdirToRoot() {
    final String target = workDirectory();
    LIBC.chdir(target);
    System.setProperty("user.dir", target);
  }

  private void log(final String template, final Object... args) {
    final String message = Pretty.format(template, args);
    System.out.printf(Locale.ENGLISH, "[asio/%s] %s%n", applicationId(), message);
  }

  /**
   * Thrown if daemonizing a process fails for any reason.
   */
  public static final class DaemonizeFailed extends IllegalStateException {
    public DaemonizeFailed(final Throwable cause) {
      super(cause);
    }
  }
}
