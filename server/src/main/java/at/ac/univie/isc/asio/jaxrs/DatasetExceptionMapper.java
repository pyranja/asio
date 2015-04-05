package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Error;
import at.ac.univie.isc.asio.platform.CurrentTime;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Convert {@link DatasetException}s to responses with appropriate status and attempt to add a
 * detail message describing the cause if available.
 *
 * @author Chris Borckholder
 */
@Provider
public class DatasetExceptionMapper implements ExceptionMapper<DatasetException> {
  static final MediaType ERROR_TYPE = MediaType.valueOf(Error.MEDIA_TYPE_NAME);

  // TODO : inject correctly scoped
  private static final Correlation FIXED_PLACEHOLDER = Correlation.valueOf("none");

  private final Ticker ticker;
  private final boolean debug;

  @VisibleForTesting
  DatasetExceptionMapper(final boolean debug, final Ticker ticker) {
    this.debug = debug;
    this.ticker = ticker;
  }

  public DatasetExceptionMapper() {
    this(true, CurrentTime.instance());
  }

  @Override
  public Response toResponse(final DatasetException exception) {
    final ResponseBuilder response = selectResponseStatus(exception);
    final Error error = Error.from(exception, FIXED_PLACEHOLDER, ticker.read(), debug);
    return response.entity(error).type(ERROR_TYPE).build();
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
