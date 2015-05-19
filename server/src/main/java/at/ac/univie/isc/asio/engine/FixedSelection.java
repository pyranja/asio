/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Language;
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
