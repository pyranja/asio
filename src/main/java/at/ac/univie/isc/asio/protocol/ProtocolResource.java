package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.Command;
import at.ac.univie.isc.asio.Connector;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.security.Role;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscription;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.*;
import java.util.concurrent.TimeUnit;

@Path("/{permission}/{language}")
public class ProtocolResource {
  private static final Logger log = LoggerFactory.getLogger(ProtocolResource.class);

  @Deprecated
  public ProtocolResource() {
    throw new AssertionError("attempt to use non-managed resource");
  }

  private final Registry registry;
  private final TimeoutSpec timeout;

  public ProtocolResource(final Registry registry, final TimeoutSpec timeout) {
    this.registry = registry;
    this.timeout = timeout;
  }

  @Context
  private Request request;
  @Context
  private SecurityContext security;
  @Context
  private HttpHeaders headers;

  @PathParam("language")
  private Language language;

  @GET
  public void acceptQuery(@Context final UriInfo uri, @Suspended final AsyncResponse async) {
    final Parameters handler = Parameters
        .builder(language)
        .add(uri.getQueryParameters())
        .build(headers);
    process(async, handler);
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void acceptForm(final MultivaluedMap<String, String> formParameters,
                         @Suspended final AsyncResponse async) {
    final Parameters handler = Parameters
        .builder(language)
        .add(formParameters)
        .build(headers);
    process(async, handler);
  }

  @POST
  public void acceptBody(final String body,
                         @HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType contentType,
                         @Suspended final AsyncResponse async) {
    Parameters handler = Parameters
        .builder(language)
        .body(body, contentType)
        .build(headers);
    process(async, handler);
  }

  @Path("/schema")
  @GET
  public void serveSchema(@Suspended final AsyncResponse async) {
    final Parameters handler = Parameters
        .builder(language)
        .single("schema", "schema")
        .build(headers);
    process(async, handler);
  }

  private void process(final AsyncResponse async, final Parameters params) {
    try {
      log.debug("request parameters : {}", params);
      params.failIfNotValid();
      final Connector connector = registry.find(language);
      final Command executable = connector.createCommand(params, security.getUserPrincipal());
      checkAuthorization(executable.requiredRole());
      final Subscription subscription = executable.observe().subscribe(
          OperationObserver.bridgeTo(async).send(Response.ok().type(executable.format()))
      );
      async.setTimeout(timeout.getAs(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
      async.setTimeoutHandler(new UnsubscribeOnTimeout(subscription));
    } catch (final Throwable t) {
      resumeWithError(async, t);
    }
  }

  private void checkAuthorization(final Role required) {
    boolean authorized = security.isUserInRole(required.name());
    if (HttpMethod.GET.equals(request.getMethod())) { // restrict to READ permission
      authorized = Permission.READ.grants(required) && authorized;
    }
    if (!authorized) {
      throw new ForbiddenException();
    }
  }

  /**
   * log the occurred error and resume the response if it is still suspended.
   */
  private void resumeWithError(final AsyncResponse response, final Throwable error) {
    final boolean errorSent = response.resume(error);
    if (!errorSent) { log.warn("request failed - could not send error response"); }
    if (isRegular(error)) {
      final Throwable root = Throwables.getRootCause(error);
      log.warn("request failed - {}", root.getMessage());
    } else {
      log.error("request failed - {}", error.getMessage(), error);
    }
  }

  private boolean isRegular(final Throwable error) {
    return error instanceof DatasetException || error instanceof WebApplicationException;
  }

  private static class UnsubscribeOnTimeout implements TimeoutHandler {

    private final Subscription subscription;

    public UnsubscribeOnTimeout(final Subscription subscription) {
      this.subscription = subscription;
    }

    @Override
    public void handleTimeout(final AsyncResponse asyncResponse) {
      asyncResponse.resume(new ServiceUnavailableException("execution time limit exceeded"));
      subscription.unsubscribe();
    }
  }
}
