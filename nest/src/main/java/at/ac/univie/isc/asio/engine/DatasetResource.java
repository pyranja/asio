package at.ac.univie.isc.asio.engine;

import at.ac.univie.isc.asio.Dataset;
import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.Scope;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.metadata.SchemaDescriptor;
import at.ac.univie.isc.asio.security.AuthTools;
import at.ac.univie.isc.asio.tool.Reactive;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import rx.Subscription;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import java.net.URI;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

/**
 * Serve metadata on and execute operations targeting a dataset.
 * Operation execution is asynchronously and total time may be limited.
 */
@Component
@Path("/")
public class DatasetResource {
  private static final Logger log = LoggerFactory.getLogger(DatasetResource.class);

  private final Dataset dataset;
  private final Connector connector;
  private final SecurityContext security;
  private final TimeoutSpec timeout;

  @Autowired
  DatasetResource(final Dataset dataset, final Connector connector,
                  final SecurityContext security, final TimeoutSpec timeout) {
    this.dataset = dataset;
    this.connector = connector;
    this.security = security;
    this.timeout = timeout;
  }

  // === metadata ==================================================================================

  /**
   * Retrieve a descriptor of this dataset's metadata.
   */
  @GET
  @Path("/meta")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @PreAuthorize("hasAuthority('PERMISSION_ACCESS_METADATA')")
  public Response fetchMetadata() {
    log.trace(Scope.REQUEST.marker(), "serve descriptor of {}", dataset.name());
    final Optional<SchemaDescriptor> descriptor = Reactive.asOptional(dataset.metadata());
    return descriptor.isPresent()
        ? Response.ok(descriptor.get()).build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  /**
   * Retrieve the definition of this dataset, if it is based on a relational database.
   */
  @GET
  @Path("/schema")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @PreAuthorize("hasAuthority('PERMISSION_ACCESS_METADATA')")
  public Response fetchDefinition() {
    log.trace(Scope.REQUEST.marker(), "serve definition of {}", dataset.name());
    final Optional<SqlSchema> definition = Reactive.asOptional(dataset.definition());
    return definition.isPresent()
        ? Response.ok(definition.get()).build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  @GET
  @Path("/meta/schema")
  public Response redirectFromDeprecatedDefinitionUri(@Context final UriInfo uri) {
    final String requested = uri.getAbsolutePath().toString();
    final String redirect = requested.replaceFirst("/meta/schema/?$", "/schema");
    log.debug(Scope.REQUEST.marker(), "redirecting request to deprecated schema path {} to {}", requested, redirect);
    assert requested.endsWith("/meta/schema") || requested.endsWith("/meta/schema/")
        : "check @Path annotation - unexpected request to " + requested;
    return Response.status(Response.Status.MOVED_PERMANENTLY).location(URI.create(redirect)).build();
  }

  // === query operations ==========================================================================

  /**
   * Process a URI-based protocol request.
   *
   * @param uri   contains request arguments as query parameters
   * @param async response continuation
   */
  @GET
  @Path("/{language:(sql|sparql)}")
  @SuppressWarnings("VoidMethodAnnotatedWithGET")
  public void acceptQuery(@Context final UriInfo uri, @Suspended final AsyncResponse async,
                          @BeanParam final Params params) {
    log.trace(Scope.REQUEST.marker(), "serve read-only query operation on {}", dataset.name());
    process(async, parse(params).argumentsFrom(uri.getQueryParameters()).collect());
  }

  /**
   * Process a form-based protocol request.
   *
   * @param form  containing all request arguments
   * @param async response continuation
   */
  @POST
  @Path("/{language:(sql|sparql)}")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void acceptForm(final MultivaluedMap<String, String> form, @Suspended final AsyncResponse async,
                         @BeanParam final Params params) {
    log.trace(Scope.REQUEST.marker(), "serve form operation on {}", dataset.name());
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
  @Path("/{language:(sql|sparql)}")
  public void acceptBody(final String body, @HeaderParam(HttpHeaders.CONTENT_TYPE) MediaType contentType,
                         @Suspended final AsyncResponse async, @BeanParam final Params params) {
    log.trace(Scope.REQUEST.marker(), "serve direct operation on {}", dataset.name());
    process(async, parse(params).body(body, contentType).collect());
  }

  /**
   * Initialize a command parser from the request context.
   *
   * @param params shared request parameters
   * @return initialized parser
   */
  private ParseJaxrsCommand parse(final Params params) {
    Principal principal = AuthTools.findIdentity(security);
    return ParseJaxrsCommand.with(dataset.name(), params.language)
        .including(params.headers).initiatedBy(principal);
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

  /**
   * aggregate shared method parameters
   */
  @SuppressWarnings("RSReferenceInspection")
  static final class Params {
    @PathParam("language")
    public Language language;
    @Context
    public HttpHeaders headers;
  }
}

