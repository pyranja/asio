/*
 * #%L
 * asio cli
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
package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Pigeon;
import at.ac.univie.isc.asio.tool.Pretty;

import java.io.IOException;
import java.util.List;

final class Undeploy implements Command {
  private final Appendable console;
  private final Pigeon pigeon;

  Undeploy(final Appendable console, final Pigeon pigeon) {
    this.console = console;
    this.pigeon = pigeon;
  }

  @Override
  public String toString() {
    return "undeploy the container with given id if present - expects exactly one argument";
  }

  @Override
  public int call(final List<String> arguments) throws IOException {
    if (arguments.size() != 1) {
      throw new IllegalArgumentException("only a single argument allowed - the container id");
    }
    final Id target = Id.valueOf(arguments.get(0));
    final boolean success = pigeon.undeploy(target);
    if (success) {
      console.append(Pretty.format("'%s' undeployed%n", target));
      return 0;
    } else {
      console.append(Pretty.format("no container named '%s' present%n", target));
      return 1;
    }
  }
}
