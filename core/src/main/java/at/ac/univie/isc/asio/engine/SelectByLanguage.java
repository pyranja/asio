package at.ac.univie.isc.asio.engine;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Select and delegate to an {@code Engine}, that is capable of handling the required {@code Languages}.
 */
public final class SelectByLanguage implements Invoker {
  private final Map<Language, Engine> engines;

  SelectByLanguage(final Map<Language, Engine> engines) {
    this.engines = engines;
  }

  public static SelectByLanguage from(final Iterable<Engine> engines) {
    final ImmutableMap<Language, Engine> mapping =
        Maps.uniqueIndex(engines, new Function<Engine, Language>() {
          @Override
          public Language apply(final Engine input) {
            return input.language();
          }
        });
    return new SelectByLanguage(mapping);
  }

  @Override
  public Invocation prepare(final Parameters parameters) {
    final Engine delegate = engines.get(parameters.language());
    if (delegate == null) {
      throw new Language.NotSupported(Language.UNKNOWN);
    }
    return delegate.prepare(parameters);
  }
}
