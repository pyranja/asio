package at.ac.univie.isc.asio.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventor;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionTrackingMode;

/**
 * Programmatic replacement for {@code web.xml}.
 */
@SuppressWarnings("UnusedDeclaration")
public class AsioInitializer implements WebApplicationInitializer {
  private static final Logger log = LoggerFactory.getLogger(AsioInitializer.class);

  static final ImmutableMap<String, String> CXF_PARAMETERS = ImmutableMap.of(
      "static-resources-list", "/explore/.*"
      , "redirects-list", "/meta/events"
      , "redirect-servlet-name", "event-stream-servlet"
      , "redirect-attributes", "javax.servlet.include.request_uri"
  );

  @Override
  public void onStartup(final ServletContext container) throws ServletException {
    // enforce statelessness
    container.setSessionTrackingModes(ImmutableSet.<SessionTrackingMode>of());
    // leak preventor
    container.setInitParameter("ClassLoaderLeakPreventor.threadWaitMs", "1000");
    container.addListener(ClassLoaderLeakPreventor.class);
    // spring
    final AnnotationConfigWebApplicationContext spring = new AnnotationConfigWebApplicationContext();
    spring.scan("at.ac.univie.isc.asio.config");
    container.addListener(new ContextLoaderListener(spring));
    container.addListener(RequestContextListener.class);  // enables spring request scopes
    // cxf dispatcher
    final ServletRegistration.Dynamic cxf = container.addServlet("cxf-dispatcher", new CXFServlet());
    cxf.addMapping("/*");
    cxf.setLoadOnStartup(1);
    cxf.setAsyncSupported(true);
    cxf.setInitParameters(CXF_PARAMETERS);
    log.info(AsioConfiguration.SYSTEM, "web app configuration completed");
  }
}
