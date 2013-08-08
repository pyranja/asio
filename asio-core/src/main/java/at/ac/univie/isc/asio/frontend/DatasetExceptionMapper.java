package at.ac.univie.isc.asio.frontend;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;

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

  @Override
  public Response toResponse(final DatasetException error) {
    ResponseBuilder response;
    if (error instanceof DatasetUsageException) {
      response = Response.status(BAD_REQUEST);
    } else {
      response = Response.serverError();
    }
    final String message =
        format(ENGLISH, ERROR_MESSAGE, error.getLocalizedMessage(), error.failedOperation()
            .orNull(), getRootCause(error), getStackTraceAsString(error));
    return response.entity(message).type(TEXT_PLAIN_TYPE).build();

  }
}
