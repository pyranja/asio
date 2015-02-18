package at.ac.univie.isc.asio.engine;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Select an {@code Engine} from a fixed set of candidates, matching by supported language.
 */
public final class FixedSelection implements EngineRouter {
  private static final Function<Engine, Language> GET_LANGUAGE =
      new Function<Engine, Language>() {
        @Nullable
        @Override
        public Language apply(final Engine input) {
          return input.language();
        }
      };

  private final Map<Language, Engine> candidates;

  private FixedSelection(final Iterable<Engine> engines) {
    candidates = Maps.uniqueIndex(engines, GET_LANGUAGE);
  }

  public static FixedSelection from(final Iterable<Engine> engines) {
    return new FixedSelection(engines);
  }

  @Override
  public Engine select(final Command command) throws Language.NotSupported {
    final Language requested = command.language();
    final Engine found = candidates.get(requested);
    if (found == null) {
      throw new Language.NotSupported(requested);
    }
    return found;
  }
}
