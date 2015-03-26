package at.ac.univie.isc.asio.container.nest;

import com.google.common.base.Function;

import javax.annotation.Nonnull;

/**
 * Post process settings of a container, e.g. to override settings or replace missing with defaults.
 * Settings may be modified in-place on the mutable configuration beans or a fresh conig instance
 * may be returned.
 */
public interface Configurer extends Function<NestConfig, NestConfig> {

  /**
   * Post process the given configuration.
   *
   * @param input initial config
   * @return processed, possibly altered config
   */
  @Nonnull
  @Override
  NestConfig apply(NestConfig input);
}
