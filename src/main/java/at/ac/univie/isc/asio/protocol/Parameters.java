package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import com.google.common.base.Objects;
import com.google.common.collect.*;

import javax.annotation.concurrent.Immutable;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Immutable
public final class Parameters {
  public static final String KEY_LANGUAGE = "language";

  public static ParametersBuilder builder(Language language) {
    return new ParametersBuilder(language);
  }

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
  private final RuntimeException cause;

  private Parameters(
      final ListMultimap<String, String> parameters,
      final List<MediaType> acceptableTypes,
      final RuntimeException cause) {
    assert ((parameters != null && acceptableTypes != null) || cause != null)
        : "invalid params but no cause given";
    this.parameters = parameters;
    this.acceptableTypes = acceptableTypes;
    this.cause = cause;
  }

  @Deprecated
  public Map<String, List<String>> props() {
    failIfNotValid();
    return Multimaps.asMap(parameters);
  }

  public Multimap<String, String> properties() {
    failIfNotValid();
    return parameters;
  }

  /**
   * @param key of required parameter
   * @return the single value of the parameter
   * @throws at.ac.univie.isc.asio.protocol.Parameters.MissingParameter if no value is given
   * @throws at.ac.univie.isc.asio.protocol.Parameters.DuplicatedParameter if multiple values are given
   * @throws at.ac.univie.isc.asio.protocol.Parameters.IllegalParameter if found value is an empty string
   */
  public String require(final String key) {
    failIfNotValid();
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
   * Identical to
   * <pre>Language.valueOf(this.require(KEY_LANGUAGE))</pre>
   */
  public Language language() {
    return Language.valueOf(require(KEY_LANGUAGE));
  }

  public List<MediaType> acceptable() {
    failIfNotValid();
    return acceptableTypes;
  }

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
        .add("properties", parameters)
        .toString();
  }

  public static class ParametersBuilder {
    private final Language language;
    private final ImmutableListMultimap.Builder<String, String> params = ImmutableListMultimap.builder();
    private final ImmutableList.Builder<MediaType> acceptedTypes = ImmutableList.builder();

    private RuntimeException cause;

    private ParametersBuilder(final Language language) {
      this.language = language;
    }

    public ParametersBuilder add(final MultivaluedMap<String, String> map) {
      if (map == null) {
        cause = new NullPointerException("illegal parameters map");
      } else {
        for (Map.Entry<String, List<String>> each : map.entrySet()) {
          params.putAll(each.getKey(), each.getValue());
        }
      }
      return this;
    }

    public ParametersBuilder single(final String key, final String value) {
      if (key == null || value == null) {
        cause = new NullPointerException("illegal parameter");
      } else {
        params.put(key, value);
      }
      return this;
    }

    static final Pattern MEDIA_SUBTYPE_PATTERN = Pattern.compile("^(\\w+)-(\\w+)$");

    public ParametersBuilder body(final String body, final MediaType content) {
      final String subtype = content != null ? content.getSubtype() : "illegal";
      final Matcher match = MEDIA_SUBTYPE_PATTERN.matcher(subtype);
      if (valid(match) && languageMatches(match.group(1)) && exists(body)) {
        params.put(match.group(2), body);
      }
      return this;
    }

    private boolean valid(Matcher match) {
      if (match.matches()) {
        return true;
      } else {
        cause =
            new NotSupportedException("illegal content type for direct operation - use 'application/{language}-{operation}'");
        return false;
      }
    }

    private boolean languageMatches(final String given) {
      if (language.name().equalsIgnoreCase(given)) {
        return true;
      } else {
        cause = new NotSupportedException(
            "illegal content type for direct operation - expected language " + language.name());
        return false;
      }
    }

    private boolean exists(final String body) {
      if (body == null) {
        cause = new BadRequestException("missing command for direct operation");
        return false;
      } else {
        return true;
      }
    }

    public ParametersBuilder accept(MediaType type) {
      if (isNotNull(type, "accept type")) { acceptedTypes.add(type); }
      return this;
    }

    public Parameters build(final HttpHeaders context) {
      if (isNotNull(context, "http headers")) {
        if (isNotNull(context.getAcceptableMediaTypes(), "accepted types")) {
          acceptedTypes.addAll(context.getAcceptableMediaTypes());
        }
      }
      return build();
    }

    public Parameters build() {
      params.put(KEY_LANGUAGE, language.name());
      return new Parameters(
          params.build(),
          acceptedTypes.build(),
          cause
      );
    }

    private boolean isNotNull(final Object that, final String message) {
      if (that == null) {
        cause = new NullPointerException(message);
        return false;
      } else {
        return true;
      }
    }
  }
}
