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

import java.io.IOException;
import java.util.List;

/**
 * Print installed version.
 */
final class Version implements Command {
  private final Appendable out;
  private final Settings config;

  Version(final Appendable out, final Settings config) {
    this.out = out;
    this.config = config;
  }

  @Override
  public String toString() {
    return "print the version";
  }

  @Override
  public int call(final List<String> arguments) throws IOException {
    out.append(config.getVersion());
    return 0;
  }
}
