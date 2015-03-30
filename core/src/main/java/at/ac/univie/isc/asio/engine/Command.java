package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Id;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.List;

/**
 * Hold properties extracted from a protocol request.
 */
@Immutable
public final class Command {
  public static final String KEY_LANGUAGE = "language";
  public static final String KEY_SCHEMA = "schema";


  public static final class MissingParameter extends DatasetUsageException {
    public MissingParameter(final String key) {
      super("required parameter "+ key +" is missing");
    }
  }

  public static final class DuplicatedParameter extends DatasetUsageException {
    public DuplicatedParameter(final String key) {
      super("duplicated parameter "+ key +" found");
    }
  }

  public static final class IllegalParameter extends DatasetUsageException {
    public IllegalParameter(final String key, final String reason) {
      super("illegal parameter "+ key +" found : "+ reason);
    }
  }

  private final ListMultimap<String, String> parameters;
  private final List<MediaType> acceptableTypes;
  private final Optional<Principal> owner;
  private final RuntimeException cause;

  public Command(final ListMultimap<String, String> parameters,
                 final List<MediaType> acceptableTypes, final Principal owner,
                 final RuntimeException cause) {
    assert ((parameters != null && acceptableTypes != null) || cause != null)
        : "invalid params but no cause given";
    this.parameters = parameters;
    this.acceptableTypes = acceptableTypes;
    this.owner = Optional.fromNullable(owner);
    this.cause = cause;
  }

  /**
   * @return all captured parameters
   */
  public Multimap<String, String> properties() {
    return parameters;
  }

  /**
   * @param key of required parameter
   * @return the single value of the parameter
   * @throws Command.MissingParameter if no value is given
   * @throws Command.DuplicatedParameter if multiple values are given
   * @throws Command.IllegalParameter if found value is an empty string
   */
  public String require(final String key) {
    final List<String> values = parameters.get(key);
    if (values.isEmpty()) {
      throw new MissingParameter(key);
    } else if (values.size() > 1) {
      throw new DuplicatedParameter(key);
    } else {
      final String value = Iterables.getOnlyElement(values);
      if (value.trim().isEmpty()) {
        throw new IllegalParameter(key, "empty value");
      }
      return value;
    }
  }

  /**
   * Identical to <pre>Language.valueOf(this.require(KEY_LANGUAGE))</pre>
   *
   * @return requested language
   */
  public Language language() {
    return Language.valueOf(require(KEY_LANGUAGE));
  }

  /**
   * Identical to <pre>SchemaIdentifier.valueOf(require(KEY_SCHEMA))</pre>
   *
   * @return requested schema
   */
  public Id schema() {
    return Id.valueOf(require(KEY_SCHEMA));
  }

  /**
   * @return initiator of the request if known
   */
  public Optional<Principal> owner() {
    return owner;
  }

  /**
   * @return all accepted mime types sorted by preference
   */
  public List<MediaType> acceptable() {
    return acceptableTypes;
  }

  /**
   * Throw an exception, which may have occurred during construction.
   * @throws java.lang.Exception any validation error
   */
  public void failIfNotValid() {
    if (cause != null) {
      throw cause;
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .omitNullValues()
        .add("error", cause)
        .addValue(parameters)
        .add("owner", owner.orNull())
        .toString();
  }

}
