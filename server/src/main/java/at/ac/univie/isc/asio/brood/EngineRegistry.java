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
package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.EngineRouter;

import java.util.Set;

/**
 * An {@code EngineRouter}, that uses an internal mapping of {@code Schema} to {@code Container}
 * pairs to find the right {@code Engine}. The mapping may be modified by raising appropriate
 * {@link ContainerEvent events}.
 */
@Brood
final class EngineRegistry extends BaseContainerRegistry implements EngineRouter {
  public EngineRegistry() {
    log.info(Scope.SYSTEM.marker(), "engine registry enabled");
  }

  @Override
  public Engine select(final Command command) throws Language.NotSupported {
    final Container container = find(command.schema());
    return match(command.language(), container.engines());
  }

  private Engine match(final Language language, final Set<Engine> candidates) {
    for (Engine engine : candidates) {
      if (engine.language().equals(language)) {
        return engine;
      }
    }
    throw new Language.NotSupported(language);
  }
}
