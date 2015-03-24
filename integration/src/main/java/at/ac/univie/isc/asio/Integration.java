package at.ac.univie.isc.asio;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit category for integration tests.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface Integration {
  /** the identifier of integration datasets */
  public static final String IDENTIFIER = "urn:asio:dataset:integration";
  /** the base resource uri of integration mappings */
  public static final String BASE_URI = "http://example.com/asio/integration/";
}
