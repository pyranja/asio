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

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.List;

/**
 * A command in an invalid state. This does not contain any properties, but just the list of
 * violations. Invoking any method will trigger an exception, that indicates the cause of the
 * invalid state.
 */
@Immutable
final class InvalidCommand extends Command {
  private final RuntimeException cause;

  InvalidCommand(final RuntimeException cause) {
    this.cause = cause;
  }

  private RuntimeException fail() {
    throw cause;
  }

  @Override
  public void failIfNotValid() throws RuntimeException {
    throw fail();
  }

  @Override
  public String toString() {
    return "InvalidCommand{cause=" + cause + '}';
  }

  // === implementation fails fast on any gettter

  @Override
  public Multimap<String, String> properties() {
    throw fail();
  }

  @Override
  public List<MediaType> acceptable() {
    throw fail();
  }

  @Override
  public Optional<Principal> owner() {
    throw fail();
  }
}
