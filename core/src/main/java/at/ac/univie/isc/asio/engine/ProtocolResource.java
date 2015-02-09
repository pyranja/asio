package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;

import javax.ws.rs.*;
import javax.ws.rs.container.*;
import javax.ws.rs.core.*;
import java.util.concurrent.TimeUnit;

/**
 * Accept and execute protocol requests. Execution is asynchronously and total time may be limited.
 */
@AutoFactory
public class ProtocolResource {
  private static final Logger log = LoggerFactory.getLogger(ProtocolResource.class);

  private final Connector connector;
  private final TimeoutSpec timeout;
  private final ParseJaxrsCommand parser;

  ProtocolResource(final ParseJaxrsCommand parser, final Connector connector, @Provided final TimeoutSpec timeout) {
    this.connector = connector;
    this.parser = parser;
    this.timeout = timeout;
  }

  /**
   * Process a URI-based protocol request.
   * @param uri contains request arguments as query parameters
   * @param async response continuation
   */
  @GET
  @SuppressWarnings("VoidMethodAnnotatedWithGET")
  public void acceptQuery(@Context final UriInfo uri, @Suspended final AsyncResponse async) {
    process(async, parser.argumentsFrom(uri.getQueryParameters()).collect());
  }

  /**
   * Process a form-based protocol request.
   * @param form containing all request arguments
   * @param async response continuation
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void acceptForm(final MultivaluedMap<String, String> form, @Suspended final AsyncResponse async) {
    process(async, parser.argumentsFrom(form).collect());
  }

  /**
   * Process a protocol request transmitted directly in the request body.
   * @param body contains requested command
   * @param contentType signals requested operation
   * @param async response continuation
   */
  @POST
  public void acceptBody(final String body, @HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType contentType,
                         @Suspended final AsyncResponse async) {
    process(async, parser.body(body, contentType).collect());
  }

  /**
   * Invoke request processing and observe the results.
   * @param async response continuation
   * @param command parsed request
   */
  private void process(final AsyncResponse async, final Command command) {
    try {
      final Subscription subscription = connector.accept(command).subscribe(SendResults.to(async));
      AsyncListener.cleanUp(subscription).listenTo(async);
      async.setTimeout(timeout.getAs(TimeUnit.NANOSECONDS, 0L), TimeUnit.NANOSECONDS);
    } catch (final Throwable error) {
      resumeWithError(async, error);
      throw error;  // try to trigger uncaught exception handlers
    }
  }

  /**
   * Resume the AsyncResponse to avoid stalling the client on an error. Just throwing from the async
   * method will not abort the request.
   * @param response possibly suspended async response
   * @param error failure that occurred
   */
  private void resumeWithError(final AsyncResponse response, final Throwable error) {
    final Throwable wrapped = DatasetException.wrapIfNecessary(error);
    if (!response.resume(wrapped)) { log.warn("request failed - could not send error response"); }
  }

}
