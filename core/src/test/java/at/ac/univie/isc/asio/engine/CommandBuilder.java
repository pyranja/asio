package at.ac.univie.isc.asio.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import javax.ws.rs.core.MediaType;
import java.security.Principal;

/**
 * Construct {@link Command} instances.
 */
public final class CommandBuilder {
  /**
   * Initiate builder for request in given language.
   *
   * @param language of request
   * @return builder instance
   */
  public static CommandBuilder with(final Language language) {
    return new CommandBuilder(language);
  }

  /**
   * A command that is invalid due to the given cause.
   *
   * @param cause wrapped error
   * @return invalid command
   */
  public static Command invalid(final RuntimeException cause) {
    return new Command(null, null, null, cause);
  }

  /**
   * An empty command with {@link at.ac.univie.isc.asio.engine.Language#UNKNOWN unknown language}.
   *
   * @return dummy command
   */
  public static Command dummy() {
    return with(Language.UNKNOWN).build();
  }

  private final Language language;
  private final ImmutableListMultimap.Builder<String, String> arguments;
  private final ImmutableList.Builder<MediaType> accepted;
  private Principal owner;

  private CommandBuilder(final Language language) {
    this.language = language;
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

  public Command build() {
    arguments.put(Command.KEY_LANGUAGE, language.name());
    return new Command(arguments.build(), accepted.build(), owner, null);
  }
}
