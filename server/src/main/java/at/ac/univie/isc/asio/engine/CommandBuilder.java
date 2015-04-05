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
