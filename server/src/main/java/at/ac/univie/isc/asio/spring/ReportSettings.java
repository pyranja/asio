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
package at.ac.univie.isc.asio.spring;

import at.ac.univie.isc.asio.AsioSettings;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.tool.Pretty;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("unused")
@Component
final class ReportSettings {
  private static final Logger log = getLogger(ReportSettings.class);

  private final Environment environment;
  private final AsioSettings settings;

  @Autowired
  public ReportSettings(final Environment environment, final AsioSettings settings) {
    this.environment = environment;
    this.settings = settings;
  }

  private static final String MESSAGE_TEMPLATE =
      "%n%n ===== active profiles: %s ===== %n%s%n%n";

  @PostConstruct
  public void report() {
    final String message = Pretty.format(
        MESSAGE_TEMPLATE, Arrays.toString(environment.getActiveProfiles()), settings
    );
    log.info(Scope.SYSTEM.marker(), message);
  }
}
