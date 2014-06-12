package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Language;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;

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

  private final ImmutableListMultimap<String, String> parameters;
  private final HttpHeaders headers;
  private final RuntimeException cause;

  private Parameters(final ImmutableListMultimap<String, String> parameters, final HttpHeaders headers, final RuntimeException cause) {
    assert (parameters != null || cause != null) : "invalid params but no cause given";
    this.headers = headers;
    this.parameters = parameters;
    this.cause = cause;
  }

  public Map<String, List<String>> properties() {
    failIfNotValid();
    return Multimaps.asMap(parameters);
  }

  public List<MediaType> acceptable() {
    failIfNotValid();
    return headers.getAcceptableMediaTypes();
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
        .add("headers", headers.getRequestHeaders())
        .toString();
  }

  public static class ParametersBuilder {
    private ImmutableListMultimap.Builder<String, String> params = ImmutableListMultimap.builder();
    private RuntimeException cause;
    private final Language language;

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

    static final Pattern MEDIA_SUBTYPE_PATTERN = Pattern.compile("^(\\w+)-(\\w+)$");

    public ParametersBuilder body(final String body, final MediaType content) {
      final String subtype = content != null ? content.getSubtype() : "illegal";
      final Matcher match = MEDIA_SUBTYPE_PATTERN.matcher(subtype);
      if (valid(match) && languageMatches(match.group(1)) && exists(body)) {
        params.put(match.group(2), body);
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

    private boolean valid(Matcher match) {
      if (match.matches()) {
        return true;
      } else {
        cause = new NotSupportedException("illegal content type for direct operation - use 'application/{language}-{operation}'");
        return false;
      }
    }

    private boolean languageMatches(final String given) {
      if (language.name().equalsIgnoreCase(given)) {
        return true;
      } else {
        cause = new NotSupportedException("illegal content type for direct operation - expected language "+ language.name());
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

    public Parameters build(final HttpHeaders headers) {
      if (headers == null) { cause = new NullPointerException("headers"); }
      params.put(KEY_LANGUAGE, language.name());
      return new Parameters(params.build(), headers, cause);
    }
  }
}
