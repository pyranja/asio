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

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Language;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import javax.ws.rs.core.MediaType;
import java.security.Principal;

/**
 * Construct {@link Command} instances.
 */
public final class CommandBuilder {

  /**
   * Start building a command from an empty initial state.
   */
  public static CommandBuilder empty() {
    return new CommandBuilder();
  }

  private final ImmutableListMultimap.Builder<String, String> arguments;
  private final ImmutableList.Builder<MediaType> accepted;
  private Principal owner;

  private CommandBuilder() {
    accepted = ImmutableList.builder();
    arguments = ImmutableListMultimap.builder();
  }

  public CommandBuilder single(final String key, final String value) {
    arguments.put(key, value);
    return this;
  }

  public CommandBuilder accept(final MediaType type) {
    accepted.add(type);
    return this;
  }

  public CommandBuilder owner(final Principal owner) {
    this.owner = owner;
    return this;
  }

  public CommandBuilder language(final Language language) {
    arguments.put(Command.KEY_LANGUAGE, language.name());
    return this;
  }

  public CommandBuilder target(final Id target) {
    arguments.put(Command.KEY_SCHEMA, target.asString());
    return this;
  }

  public Command build() {
    return Command.create(arguments.build(), accepted.build(), owner);
  }
}
