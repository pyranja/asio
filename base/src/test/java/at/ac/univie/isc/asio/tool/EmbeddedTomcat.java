package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.jaxrs.ManagedClient;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.servlet.Servlet;
import javax.ws.rs.client.WebTarget;
import java.io.File;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Start an embedded Tomcat instance hosting servlets or a complete web application.
 */
public class EmbeddedTomcat extends ExternalResource {
  public static final String LOCALHOST = "localhost";
  public static final int PORT = 1337;
  public static final URI ADDRESS = URI.create(String.format(Locale.ENGLISH, "http://%s:%s/", LOCALHOST, PORT));

  public static EmbeddedTomcat with(final String name, final Servlet servlet) {
    return new EmbeddedTomcat(ImmutableMap.of(name, servlet));
  }

  private final Map<String, Servlet> servlets;
  private final TemporaryFolder temporaryDirectory;
  private final ManagedClient client;
  private final Tomcat tomcat;
  private boolean silent = true;

  private EmbeddedTomcat(final Map<String, Servlet> servlets) {
    this.servlets = servlets;
    this.temporaryDirectory = new TemporaryFolder();
    tomcat = new Tomcat();
    client = ManagedClient.create().build(ADDRESS);
  }

  public URI address() {
    return ADDRESS;
  }

  public WebTarget endpoint() {
    return client.endpoint();
  }

  public EmbeddedTomcat enableLogging() {
    silent = false;
    return this;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    final Statement inner = client.apply(base, description);
    final Statement tomcat = super.apply(inner, description);
    return temporaryDirectory.apply(tomcat, description);
  }

  @Override
  protected void before() throws Throwable {
    final File workDirectory = temporaryDirectory.newFolder();
    tomcat.setBaseDir(workDirectory.getAbsolutePath());
    tomcat.setPort(PORT);
    tomcat.setHostname(LOCALHOST);
    tomcat.setSilent(silent);
    if (silent) {
      Logger.getLogger("org.apache").setLevel(Level.OFF);
    }
    final Context context = tomcat.addContext("/", workDirectory.getAbsolutePath());
    for (Map.Entry<String, Servlet> each : servlets.entrySet()) {
      Tomcat.addServlet(context, each.getKey(), each.getValue());
      context.addServletMapping("/" + each.getKey(), each.getKey());
    }
    tomcat.start();
  }

  @Override
  protected void after() {
    try {
      tomcat.stop();
      tomcat.destroy();
    } catch (LifecycleException e) {
      Throwables.propagate(e);
    }
  }
}
