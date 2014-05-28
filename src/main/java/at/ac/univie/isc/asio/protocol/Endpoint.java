package at.ac.univie.isc.asio.protocol;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.frontend.ContentNegotiator;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;
import at.ac.univie.isc.asio.frontend.OperationObserver;
import at.ac.univie.isc.asio.frontend.VariantConverter;
import at.ac.univie.isc.asio.security.Anonymous;
import at.ac.univie.isc.asio.transport.ObservableStream;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.*;
import java.security.Principal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("")
public class Endpoint {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(Endpoint.class);

  private final static Set<Action> READ_ONLY = EnumSet.of(Action.QUERY);
  private final static Set<Action> READ_WRITE = EnumSet.of(Action.QUERY, Action.UPDATE);

  private final TimeoutSpec timeout;

  // dependencies
  private final OperationParser parse;
  private final ContentNegotiator content;
  private final Engine backend;
  private final VariantConverter convert;


  @Context
  private Request request;
  @Context
  private HttpHeaders headers;
  // sec
  private Principal owner = Anonymous.INSTANCE;
  private Set<Action> permissions = Collections.emptySet();

  public Endpoint(final Engine acceptor, final OperationParser parser, final ContentNegotiator negotiator,
                  final VariantConverter convert, final TimeoutSpec timeout) {
    parse = parser;
    content = negotiator;
    backend = acceptor;
    this.convert = convert;
    this.timeout = timeout;
  }

  // FIXME : use container injection
  Endpoint inject(final Request request, final HttpHeaders headers) {
    this.request = request;
    this.headers = headers;
    return this;
  }

  Endpoint authorize(final Principal owner, final Set<Action> permissions) {
    this.owner = owner;
    this.permissions = permissions;
    return this;
  }

  @GET
  public void acceptQuery(@Context final UriInfo uri, @Suspended final AsyncResponse response) {
    log.debug(">> handling GET request");
    try {
      final MultivaluedMap<String, String> queryParameters = uri.getQueryParameters();
      final Set<Action> allowed = READ_ONLY;
      process(response, queryParameters, allowed);
    } catch (final Throwable t) {
      handleError(response, t);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public void acceptForm(final MultivaluedMap<String, String> formParameters,
      @Suspended final AsyncResponse response) {
    log.debug(">> handling FORM request");
    try {
      final Set<Action> allowed = READ_WRITE;
      process(response, formParameters, allowed);
    } catch (final Throwable t) {
      handleError(response, t);
    }
  }

  @POST
  public void acceptBody(final String body, @Suspended final AsyncResponse response) {
    log.debug(">> handling RAW request");
    try {
      final MediaType type = headers.getMediaType();
      final String actionText = extractActionTextFromMediaType(type);
      final MultivaluedMap<String, String> bodyParameters = new MultivaluedHashMap<>(2);
      bodyParameters.add(actionText, body);
      final Set<Action> allowed = READ_WRITE;
      process(response, bodyParameters, allowed);
    } catch (final Throwable t) {
      handleError(response, t);
    }
  }

  @Path("/schema")
  @GET
  public void serveSchema(@Suspended final AsyncResponse async) {
    log.debug(">> handling SCHEMA request");
    try {
      final OperationBuilder op = parse.operationForAction(Action.SCHEMA);
      final SerializationFormat format = content.negotiate(request, Action.SCHEMA);
      final DatasetOperation operation = op.renderAs(format);
      final Observable<ObservableStream> execution = backend.execute(operation);
      final Response.ResponseBuilder response =
          Response.ok().type(convert.asContentType(format.asMediaType()));
      final Subscription subscription = execution.subscribe(new OperationObserver(async, response));
      async.setTimeout(timeout.getAs(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
      async.setTimeoutHandler(new UnsubscribeOnTimeout(subscription));
    } catch (final Throwable t) {
      handleError(async, t);
    }
  }

  private void process(final AsyncResponse async,
      final MultivaluedMap<String, String> parameters, final Set<Action> allowed) {
    log.debug("processing request with parameters {} expecting one of {}", parameters, allowed);
    final Set<Action> authorized = Sets.intersection(allowed, permissions);
    final OperationBuilder op = parse.operationFromParameters(parameters, authorized);
    final SerializationFormat format = content.negotiate(request, op.getAction());
    final DatasetOperation operation = op.renderAs(format).withOwner(owner);
    final Observable<ObservableStream> execution = backend.execute(operation);
    final Response.ResponseBuilder response =
        Response.ok().type(convert.asContentType(format.asMediaType()));
    final Subscription subscription = execution.subscribe(new OperationObserver(async, response));
    async.setTimeout(timeout.getAs(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    async.setTimeoutHandler(new UnsubscribeOnTimeout(subscription));
  }

  static final Pattern MEDIA_SUBTYPE_PATTERN = Pattern.compile("^(\\w+)-(\\w+)$");

  /**
   * TODO extract to functional interface class ***
   * ContentTypeRecognizer#recognize(mediatype):{Language, Action}
   */
  private String extractActionTextFromMediaType(final MediaType type) {
    final Matcher match = MEDIA_SUBTYPE_PATTERN.matcher(type.getSubtype());
    if (match.matches()) {
      return match.group(2);
    } else {
      throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }
  }

  /**
   * log the occurred error and resume the response if it is still suspended.
   */
  private void handleError(final AsyncResponse response, final Throwable error) {
    response.setTimeout(1, TimeUnit.SECONDS); // TODO : y does it not fail immediately on resume?
    if (error instanceof DatasetException || error instanceof WebApplicationException) {
      log.warn("exception on request processing : {}", error.getMessage());
    } else {
      log.error("unexpected failure on request processing", error);
    }
    if (response.isDone()) {
      log.warn("request already processed - cannot send error", error);
    } else {
      response.resume(error);
    }
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
