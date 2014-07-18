package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import at.ac.univie.isc.asio.transfer.ErrorMessage;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

/**
 * Convert {@link DatasetException}s to responses with appropriate status and attempt to add a
 * detail message describing the cause if available.
 *
 * @author Chris Borckholder
 */
@Provider
public class DatasetExceptionMapper implements ExceptionMapper<DatasetException> {

  // @formatter:off
	private static final String ERROR_MESSAGE =
			"[ERROR] %s\n" 	+ 	// top level message
			"[OP] %s\n" 	+	// failed operation
			"[CAUSE] %s\n" 	+ 	// root level message
			"[TRACE] %s"	;	// stack trace
	// @formatter:on

  private final List<Variant> variants;

  @Context
  private Request request;

  public DatasetExceptionMapper() {
    variants =
        Variant.mediaTypes(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE).add().build();
  }

  @Override
  public Response toResponse(final DatasetException error) {
    final ResponseBuilder response = selectResponseStatus(error);
    Variant selected = request.selectVariant(variants);
    // TODO : extract text/plain MessageBodyWriter for ErrorMessage
    if (selected == null) {
      // fall back to plain text
      final String message =
          format(ENGLISH, ERROR_MESSAGE, error.getLocalizedMessage(), error.failedOperation()
              .orNull(), getRootCause(error), getStackTraceAsString(error));
      return response.entity(message).type(TEXT_PLAIN_TYPE).build();
    } else {
      final ErrorMessage message = new ErrorMessage().withMessage(error.getLocalizedMessage())
          .withOperation(Objects.toString(error.failedOperation().orNull()))
          .withCause(getRootCause(error).getLocalizedMessage())
          .withTrace(getStackTraceAsString(error));
      return response.entity(message).type(selected.getMediaType()).build();
    }
  }

  private static final Map<Class<?>, Response.Status> ERROR_CODE_LOOKUP = ImmutableMap
      .<Class<?>, Response.Status>builder()
      .put(Connector.LanguageNotSupported.class, Response.Status.NOT_FOUND)
      .put(TypeMatchingResolver.NoMatchingFormat.class, Response.Status.NOT_ACCEPTABLE)
      .build();

  /**
   * Set 4xx or 5xx error codes appropriate to the exception type.
   *
   * @param error to be serialized
   * @return ResponseBuilder with appropriate status code set.
   */
  private ResponseBuilder selectResponseStatus(final DatasetException error) {
    Response.Status status = ERROR_CODE_LOOKUP.get(error.getClass());
    if (status == null) {
      status = defaultStatus(error);
    }
    return Response.status(status);
  }

  private Response.Status defaultStatus(final DatasetException error) {
    return (error instanceof DatasetUsageException)
        ? Response.Status.BAD_REQUEST
        : Response.Status.INTERNAL_SERVER_ERROR;
  }
}
