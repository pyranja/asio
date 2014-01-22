package at.ac.univie.isc.asio.protocol;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.Language;

@Path("/{language}")
public class EntryPoint {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(EntryPoint.class);

  @Context
  private Request request;
  @Context
  private HttpHeaders headers;

  private final EndpointSupplier endpoints;

  public EntryPoint(final EndpointSupplier endpoints) {
    super();
    this.endpoints = endpoints;
  }

  @Path("")
  public Endpoint forward(@PathParam("language") final String language) {
    log.debug(">> handling {} request", language);
    final Language lang = Language.fromString(language);
    final Endpoint target = endpoints.get(lang);
    return target.inject(request, headers);
  }
}
