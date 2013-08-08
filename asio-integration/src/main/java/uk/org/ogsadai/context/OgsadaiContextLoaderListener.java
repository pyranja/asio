package uk.org.ogsadai.context;

import java.io.File;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Initialize the {@link OGSADAIContext} on webapp start using the bean definition file specified as
 * context parameter {@link #CONFIG_FILE_PARAMETER ogsadaiConfigLocation}.
 * 
 * @author Chris Borckholder
 */
public class OgsadaiContextLoaderListener implements ServletContextListener {

  // use jdk logging to avoid initialization races
  // redirects to JULI on tomcat
  private static final Logger log = Logger.getLogger(OgsadaiContextLoaderListener.class.getName());

  // web app context parameter
  public static final String CONFIG_FILE_PARAMETER = "ogsadaiConfigLocation";
  // path extensions
  private static final String SCHEMA_PATH = "schema";
  private static final String CONFIG_PATH = "WEB-INF/etc/dai";

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    log.config("initializing ogsadai context");
    final File root = resolveRoot(event.getServletContext());
    log.config("using " + root + " as root path");
    final ApplicationContext parent;
    try {
      parent = initializeParent(root);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException("ogsadai initialization - failed to generate parent context",
          e);
    }
    final String ogsadaiConfigLocation = readConfigLocation(event.getServletContext());
    log.config("using " + ogsadaiConfigLocation + " as ogsadai context");
    try {
      OGSADAIContext.initialize(ogsadaiConfigLocation, parent);
    } catch (final Exception e) {
      throw new IllegalStateException("ogsadai initialization - failed to create OGSADAIContext", e);
    }
    OGSADAIContext.getInstance().logEntries();
  }

  /**
   * Determine the web-application's root directory
   */
  private File resolveRoot(final ServletContext context) {
    final String rootPath = context.getRealPath("/");
    return new File(rootPath);
  }

  /**
   * reads and validates the ogsadai config location parameter from the given ServletContext.
   */
  private String readConfigLocation(final ServletContext context) {
    final String location = context.getInitParameter(CONFIG_FILE_PARAMETER);
    if (location == null || location.trim().isEmpty()) {
      throw new IllegalStateException("ogsadai initialization - init param ['"
          + CONFIG_FILE_PARAMETER + "'] empty or missing in web.xml");
    }
    return location;
  }

  /**
   * Generate a parent {@link ApplicationContext} holding the dynamic references to important
   * application directories.
   * 
   * @throws ClassNotFoundException if registering directory beans fails
   */
  private ApplicationContext initializeParent(final File root) throws ClassNotFoundException {
    final GenericApplicationContext parent = new GenericApplicationContext();
    SpringUtils.registerFileBean(parent, OGSADAIConstants.WEB_APP_DIR.toString(), root);
    final File schemaDirectory = new File(root, SCHEMA_PATH);
    SpringUtils.registerFileBean(parent, OGSADAIConstants.SCHEMA_DIR.toString(), schemaDirectory);
    final File configDirectory = new File(root, CONFIG_PATH);
    SpringUtils.registerFileBean(parent, OGSADAIConstants.CONFIG_DIR.toString(), configDirectory);
    parent.refresh();
    return parent;
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    log.config("destroying ogsadai context");
    // XXX clean up needed here ?
  }
}
