package at.ac.univie.isc.asio;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Mark components that are required for the {@code brood}-profile.
 */
@Component
@Profile(Brood.PROFILE)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Brood {
  /** name of the brood profile */
  public static final String PROFILE = "brood";
}
