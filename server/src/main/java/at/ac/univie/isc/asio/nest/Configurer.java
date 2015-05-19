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
package at.ac.univie.isc.asio.nest;

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
