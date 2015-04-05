package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.tool.ValueOrError;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

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
public class ParseJaxrsCommand {
  private final Id target;
  private final Language language;
  private Principal owner;
  private final List<MediaType> acceptedTypes = Lists.newArrayList();
  private final Multimap<String, String> parameters =
      MultimapBuilder.ListMultimapBuilder.hashKeys().arrayListValues(1).build();

  private RuntimeException cause;

  private ParseJaxrsCommand(final Id target, final Language language) {
    this.target = target;
    this.language = language;
  }

  public static ParseJaxrsCommand with(final Language language) {
    return with(Id.valueOf("default"), language);
  }

  public static ParseJaxrsCommand with(final Id id, final Language language) {
    return new ParseJaxrsCommand(id, language);
  }

  public ParseJaxrsCommand argumentsFrom(final MultivaluedMap<String, String> map) {
    if (isNotNull(map, "parameters map")) {
      for (Map.Entry<String, List<String>> each : map.entrySet()) {
        parameters.putAll(each.getKey(), each.getValue());
      }
    }
    return this;
  }

  public ParseJaxrsCommand body(final String body, final MediaType content) {
    if (body == null) {
      cause = new BadRequestException("missing command for direct operation");
      return this;
    }
    final ValueOrError<String> extracted = ExtractOperation.expect(language).from(content);
    if (extracted.hasValue()) {
      parameters.put(extracted.get(), body);
    } else {
      cause = extracted.error();
    }
    return this;
  }

  public ParseJaxrsCommand withOwner(final Principal owner) {
    if (isNotNull(owner, "owner")) {
      this.owner = owner;
    }
    return this;
  }

  public ParseJaxrsCommand withHeaders(final HttpHeaders headers) {
    if (isNotNull(headers, "http headers")) {
      if (isNotNull(headers.getAcceptableMediaTypes(), "accepted types")) {
        acceptedTypes.addAll(headers.getAcceptableMediaTypes());
      }
    }
    return this;
  }

  public Command collect() {
    if (isNotNull(target, "missing target")) {
      parameters.put(Command.KEY_SCHEMA, target.asString());
    }
    if (isNotNull(language, "missing language")) {
      parameters.put(Command.KEY_LANGUAGE, language.name());
    }
    return cause == null
        ? Command.create(parameters, acceptedTypes, owner)
        : Command.invalid(cause);
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
