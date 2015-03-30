package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.security.Identity;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import rx.Subscription;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

/**
 * Accept and execute protocol requests. Execution is asynchronously and total time may be limited.
 */
@Component
@Path("/{schema}/{language:(sql|sparql)}")
public class EndpointsResource {
  private static final Logger log = LoggerFactory.getLogger(EndpointsResource.class);

  private final Connector connector;
  private final TimeoutSpec timeout;

  @Autowired
  EndpointsResource(final Connector connector, final TimeoutSpec timeout) {
    this.connector = connector;
    this.timeout = timeout;
  }

  /**
   * Process a URI-based protocol request.
   *
   * @param uri   contains request arguments as query parameters
   * @param async response continuation
   */
  @GET
  @SuppressWarnings("VoidMethodAnnotatedWithGET")
  public void acceptQuery(@Context final UriInfo uri, @Suspended final AsyncResponse async,
                          @BeanParam final Params params) {
    process(async, parse(params).argumentsFrom(uri.getQueryParameters()).collect());
  }

  /**
   * Process a form-based protocol request.
   *
   * @param form  containing all request arguments
   * @param async response continuation
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void acceptForm(final MultivaluedMap<String, String> form, @Suspended final AsyncResponse async,
                         @BeanParam final Params params) {
    process(async, parse(params).argumentsFrom(form).collect());
  }

  /**
   * Process a protocol request transmitted directly in the request body.
   *
   * @param body        contains requested command
   * @param contentType signals requested operation
   * @param async       response continuation
   */
  @POST
  public void acceptBody(final String body, @HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType contentType,
                         @Suspended final AsyncResponse async, @BeanParam final Params params) {
    process(async, parse(params).body(body, contentType).collect());
  }

  /**
   * Initialize a command parser from the request context.
   *
   * @param params shared request parameters
   * @return initialized parser
   */
  private ParseJaxrsCommand parse(final Params params) {
    Principal principal = params.security.getUserPrincipal() == null
        ? Identity.undefined()
        : params.security.getUserPrincipal();
    if (principal instanceof Authentication) {
      if (((Authentication) principal).getCredentials() instanceof Identity) {
        principal = (Identity) ((Authentication) principal).getCredentials();
      } else {
        principal = Identity.undefined();
      }
    }
    return ParseJaxrsCommand.with(params.id, params.language).including(params.headers).initiatedBy(principal);
  }

  /**
   * Invoke request processing and observe the results.
   *
   * @param async   response continuation
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
   *
   * @param response possibly suspended async response
   * @param error    failure that occurred
   */
  private void resumeWithError(final AsyncResponse response, final Throwable error) {
    final Throwable wrapped = DatasetException.wrapIfNecessary(error);
    if (!response.resume(wrapped)) { log.warn("request failed - could not send error response"); }
  }

  /** aggregate shared method parameters */
  static final class Params {
    @PathParam("schema")
    public Id id;
    @PathParam("language")
    public Language language;
    @Context
    public HttpHeaders headers;
    @Context
    public SecurityContext security;
  }
}

