package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.engine.Language;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

//import at.ac.univie.isc.asio.engine.Command;
//import at.ac.univie.isc.asio.engine.TypeMatchingResolver;

/**
 * Convert {@link DatasetException}s to responses with appropriate status and attempt to add a
 * detail message describing the cause if available.
 *
 * @author Chris Borckholder
 */
@Provider
public class DatasetExceptionMapper implements ExceptionMapper<DatasetException> {
  static final MediaType ERROR_TYPE = MediaType.valueOf("application/vnd.error+json");

  private boolean debug;

  @VisibleForTesting
  DatasetExceptionMapper(final boolean debug) {
    this.debug = debug;
  }

  public DatasetExceptionMapper() {
    this(true);
  }

  @Override
  public Response toResponse(final DatasetException error) {
    final ResponseBuilder response = selectResponseStatus(error);
    final Error dto = new Error();
    dto.setMessage(error.getLocalizedMessage());
    dto.setCause(error.toString());
    //noinspection ThrowableResultOfMethodCallIgnored
    dto.setRoot(Throwables.getRootCause(error).toString());
    if (debug) {
      dto.setTrace(Throwables.getStackTraceAsString(error));
    }
    return response.entity(dto).type(ERROR_TYPE).build();
  }

  private static final Map<Class<?>, Response.Status> ERROR_CODE_LOOKUP = ImmutableMap
      .<Class<?>, Response.Status>builder()
      .put(Language.NotSupported.class, Response.Status.NOT_FOUND)
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
