package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.*;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import at.ac.univie.isc.asio.insight.Emitter;
import at.ac.univie.isc.asio.insight.VndError;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Convert unhandled exceptions into {@link VndError} responses.
 * Additionally raise an event for each mapped exception.
 */
@Component
@Provider
public final class VndErrorMapper implements ExceptionMapper<Exception> {
  private static final Logger log = getLogger(VndErrorMapper.class);

  /**
   * media type of vnd.error responses
   */
  public static final MediaType ERROR_MIME = MediaType.valueOf(VndError.MEDIA_TYPE_NAME);

  static final Map<Class<?>, Response.Status> ERROR_CODES =
      ImmutableMap.<Class<?>, Response.Status>builder()
          .put(Language.NotSupported.class, Response.Status.NOT_FOUND)
          .put(Id.NotFound.class, Response.Status.NOT_FOUND)
          .put(TypeMatchingResolver.NoMatchingFormat.class, Response.Status.NOT_ACCEPTABLE)
          .put(AccessDeniedException.class, Response.Status.FORBIDDEN)
          .build();

  private final Emitter events;

  @Autowired
  VndErrorMapper(final Emitter emitter) {
    log.info(Scope.SYSTEM.marker(), "activate error handler with mappings {}", ERROR_CODES);
    events = emitter;
  }

  @Override
  public Response toResponse(final Exception exception) {
    final Response.StatusType status = selectErrorCode(exception);
    log.debug(Scope.REQUEST.marker(), "mapping exception {} to {} http response", exception.toString(), status);
    final VndError error = events.emit(exception);
    return Response.status(status).type(ERROR_MIME).entity(error).build();
  }

  private Response.StatusType selectErrorCode(final Exception exception) {
    final Response.StatusType selected;
    if (exception instanceof WebApplicationException) {
      selected = ((WebApplicationException) exception).getResponse().getStatusInfo();
    } else if (exception instanceof InvalidUsage) {
      selected = Response.Status.BAD_REQUEST;
    } else {
      selected = Response.Status.INTERNAL_SERVER_ERROR;
    }
    final Response.Status override = ERROR_CODES.get(exception.getClass());
    return Objects.firstNonNull(override, selected);
  }
}
