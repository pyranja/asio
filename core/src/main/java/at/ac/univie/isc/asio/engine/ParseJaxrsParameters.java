package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.tool.ValueOrError;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Build protocol request parameters from JAX-RS request elements.
 */
public class ParseJaxrsParameters {
  private final Language language;
  private final ImmutableListMultimap.Builder<String, String> params =
      ImmutableListMultimap.builder();
  private final ImmutableList.Builder<MediaType> acceptedTypes = ImmutableList.builder();
  private Principal owner;
  private RuntimeException cause;

  private ParseJaxrsParameters(final Language language) {
    this.language = language;
  }

  public static ParseJaxrsParameters with(Language language) {
    return new ParseJaxrsParameters(language);
  }

  public ParseJaxrsParameters argumentsFrom(final MultivaluedMap<String, String> map) {
    if (isNotNull(map, "parameters map")) {
      for (Map.Entry<String, List<String>> each : map.entrySet()) {
        params.putAll(each.getKey(), each.getValue());
      }
    }
    return this;
  }

  public ParseJaxrsParameters body(final String body, final MediaType content) {
    if (body == null) {
      cause = new BadRequestException("missing command for direct operation");
      return this;
    }
    final ValueOrError<String> extracted = ExtractOperation.expect(language).from(content);
    if (extracted.hasValue()) {
      params.put(extracted.get(), body);
    } else {
      cause = extracted.error();
    }
    return this;
  }

  public ParseJaxrsParameters initiatedBy(final Principal owner) {
    if (isNotNull(owner, "owner")) {
      this.owner = owner;
    }
    return this;
  }

  public ParseJaxrsParameters including(final HttpHeaders context) {
    if (isNotNull(context, "http headers")) {
      if (isNotNull(context.getAcceptableMediaTypes(), "accepted types")) {
        acceptedTypes.addAll(context.getAcceptableMediaTypes());
      }
    }
    return this;
  }

  public Parameters collect() {
    params.put(Parameters.KEY_LANGUAGE, language.name());
    return new Parameters(params.build(), acceptedTypes.build(), owner, cause);
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
