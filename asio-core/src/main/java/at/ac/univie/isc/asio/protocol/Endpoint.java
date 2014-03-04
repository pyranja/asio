package at.ac.univie.isc.asio.protocol;

import java.security.Principal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetOperation;
import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.DatasetOperation.SerializationFormat;
import at.ac.univie.isc.asio.Result;
import at.ac.univie.isc.asio.coordination.OperationAcceptor;
import at.ac.univie.isc.asio.frontend.AsyncProcessor;
import at.ac.univie.isc.asio.frontend.ContentNegotiator;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;
import at.ac.univie.isc.asio.security.Anonymous;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

@Path("")
public class Endpoint {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(Endpoint.class);

  private final static Set<Action> READ_ONLY = EnumSet.of(Action.QUERY);
  private final static Set<Action> READ_WRITE = EnumSet.of(Action.QUERY, Action.UPDATE);

  // dependencies
  private final OperationParser parse;
  private final ContentNegotiator content;
  private final OperationAcceptor backend;
  private final AsyncProcessor next;

  @Context
  private Request request;
  @Context
  private HttpHeaders headers;
  // sec
  private Principal owner = Anonymous.INSTANCE;
  private Set<Action> permissions = Collections.emptySet();

  public Endpoint(final OperationParser parser, final ContentNegotiator negotiator,
      final OperationAcceptor acceptor, final AsyncProcessor processor) {
    parse = parser;
    content = negotiator;
    backend = acceptor;
    next = processor;
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
  public void serveSchema(@Suspended final AsyncResponse response) {
    log.debug(">> handling SCHEMA request");
    try {
      final OperationBuilder op = parse.operationForAction(Action.SCHEMA);
      final SerializationFormat format = content.negotiate(request, Action.SCHEMA);
      final DatasetOperation operation = op.renderAs(format);
      final ListenableFuture<Result> result = backend.accept(operation);
      next.handle(result, response);
    } catch (final Throwable t) {
      handleError(response, t);
    }
  }

  /**
   * @param response
   * @param parameters
   * @param allowed
   */
  private void process(final AsyncResponse response,
      final MultivaluedMap<String, String> parameters, final Set<Action> allowed) {
    log.debug("processing request with parameters {} expecting one of {}", parameters, allowed);
    final Set<Action> authorized = Sets.intersection(allowed, permissions);
    final OperationBuilder op = parse.operationFromParameters(parameters, authorized);
    final SerializationFormat format = content.negotiate(request, op.getAction());
    final DatasetOperation operation = op.renderAs(format).withOwner(owner);
    final ListenableFuture<Result> result = backend.accept(operation);
    next.handle(result, response);
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
}
