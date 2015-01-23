package at.ac.univie.isc.asio.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import javax.ws.rs.core.MediaType;
import java.security.Principal;

/**
 * Construct {@link Parameters} instances.
 */
public final class ParametersBuilder {
  /**
   * Initiate builder for request in given language.
   * @param language of request
   * @return builder instance
   */
  public static ParametersBuilder with(final Language language) {
    return new ParametersBuilder(language);
  }

  public static Parameters invalid(final RuntimeException cause) {
    return new Parameters(null, null, null, cause);
  }

  private final Language language;
  private final ImmutableListMultimap.Builder<String, String> arguments;
  private final ImmutableList.Builder<MediaType> accepted;
  private Principal owner;

  private ParametersBuilder(final Language language) {
    this.language = language;
    accepted = ImmutableList.builder();
    arguments = ImmutableListMultimap.builder();
  }

  static Parameters dummy() {
    return with(Language.UNKNOWN).build();
  }

  public ParametersBuilder single(final String key, final String value) {
    arguments.put(key, value);
    return this;
  }

  public ParametersBuilder accept(final MediaType type) {
    accepted.add(type);
    return this;
  }

  public ParametersBuilder owner(final Principal owner) {
    this.owner = owner;
    return this;
  }

  public Parameters build() {
    arguments.put(Parameters.KEY_LANGUAGE, language.name());
    return new Parameters(arguments.build(), accepted.build(), owner, null);
  }
}
