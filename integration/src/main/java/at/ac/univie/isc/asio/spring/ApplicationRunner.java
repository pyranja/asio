package at.ac.univie.isc.asio.spring;

import org.junit.rules.ExternalResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;

import static java.util.Objects.requireNonNull;

/**
 * Allows to start a spring application and ensures it is stopped after the test. Also allows to
 * get the server port.
 */
public final class ApplicationRunner extends ExternalResource {
  public static ApplicationRunner run(final Object... sources) {
    return create(new SpringApplicationBuilder(sources));
  }

  public static ApplicationRunner create(final SpringApplicationBuilder builder) {
    return new ApplicationRunner(builder);
  }

  private final SpringApplicationBuilder builder;
  private ApplicationContext context;

  private ApplicationRunner(final SpringApplicationBuilder builder) {
    this.builder = builder;
  }

  public ApplicationRunner run(final String... args) {
    assert context == null : "application already started";
    context = builder.run(args);
    return this;
  }

  public int getPort() {
    requireNonNull(context, "application not started or startup failed");
    if (context instanceof AnnotationConfigEmbeddedWebApplicationContext) {
      return ((AnnotationConfigEmbeddedWebApplicationContext) context).getEmbeddedServletContainer().getPort();
    } else {
      throw new IllegalStateException("spring application is not a webapp : " + context);
    }
  }

  public <T> T property(final String key, final Class<T> clazz) {
    return context.getEnvironment().getProperty(key, clazz);
  }

  @Override
  protected void after() {
    if (context != null) {
      SpringApplication.exit(context);
    }
  }
}
