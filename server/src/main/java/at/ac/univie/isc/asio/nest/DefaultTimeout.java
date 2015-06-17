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

import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * Set the container timeout to a default value if it is missing.
 */
@Component
final class DefaultTimeout implements Configurer {
  private final Timeout fallback;

  @Autowired
  public DefaultTimeout(final Timeout fallback) {
    requireNonNull(fallback, "illegal default timeout");
    this.fallback = fallback;
  }

  @Nonnull
  @Override
  public NestConfig apply(final NestConfig input) {
    final Dataset dataset = input.getDataset();
    dataset.setTimeout(
        Objects.firstNonNull(dataset.getTimeout(), fallback).orIfUndefined(fallback)
    );
    return input;
  }

  @Override
  public String toString() {
    return "DefaultTimeout{" +
        "fallback=" + fallback +
        '}';
  }
}
