package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.InvalidUsage;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.Collection;
import java.util.List;

/**
 * Describe a dataset operation.
 */
@AutoValue
public abstract class Command {
  public static String KEY_LANGUAGE = "language";
  public static String KEY_SCHEMA = "schema";

  /** create a valid command with given arguments */
  static Command create(final Multimap<String, String> properties,
                        final List<MediaType> accepted,
                        final Principal owner) {
    return new AutoValue_Command(ImmutableMultimap.copyOf(properties),
        ImmutableList.copyOf(accepted), Optional.fromNullable(owner));
  }

  /**
   * A command that is invalid due to the given reason.
   *
   * @param cause describes why the command is invalid
   * @return invalid command
   */
  public static Command invalid(final RuntimeException cause) {
    return new InvalidCommand(cause);
  }

  /**
   * Thrown if a command misses required parameters, a parameter has an illegal value or holds an
   * invalid combination of parameters.
   */
  public static class IllegalCommand extends DatasetUsageException implements InvalidUsage {
    public IllegalCommand(final String reason) {
      super(reason);
    }
  }

  Command() { /* prevent external sub-classes */ }

  /**
   * All captured command properties.
   *
   * @return all captured parameters
   */
  public abstract Multimap<String, String> properties();

  /**
   * Ordered list of result media types, which are accepted by the client.
   *
   * @return all accepted mime types sorted by preference
   */
  public abstract List<MediaType> acceptable();

  /**
   * The identity of the client that issued this command, if it is known.
   *
   * @return initiator of the request if known
   */
  public abstract Optional<Principal> owner();

  /**
   * Validate the state of this command, if there are any inconsistencies or illegal values an
   * exception is thrown, that indicates all violations.
   *
   * @throws IllegalArgumentException if this command is in an invalid state
   */
  public void failIfNotValid() throws IllegalCommand { /* no-op */ }

  // === special property accessors

  /**
   * The target dataset of this command.
   */
  public final Id schema() {
    return Id.valueOf(require(KEY_SCHEMA));
  }

  /**
   * The payload language of this command.
   */
  public final Language language() {
    return Language.valueOf(require(KEY_LANGUAGE));
  }

  /**
   * Attempt to get the single command property with the given key, failing fast if the property is
   * either missing or there is more than one value.
   *
   * @param key key of the required property
   * @return the property value
   */
  public final String require(final String key) {
    final Collection<String> values = properties().get(key);
    if (values.isEmpty()) {
      throw new IllegalCommand("required parameter " + key + " is missing");
    } else if (values.size() > 1) {
      throw new IllegalCommand("duplicated parameter " + key + " found");
    } else {
      final String value = Iterables.getOnlyElement(values);
      if (value.trim().isEmpty()) {
        throw new IllegalCommand("illegal parameter " + key + " found : empty value");
      }
      return value;
    }
  }
}
