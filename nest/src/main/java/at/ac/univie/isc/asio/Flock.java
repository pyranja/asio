package at.ac.univie.isc.asio;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Mark components that are required for the {@code flock}-profile.
 */
@Component
@Profile(Flock.PROFILE)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Flock {
  /** name of the flock profile */
  public static final String PROFILE = "flock";
}
