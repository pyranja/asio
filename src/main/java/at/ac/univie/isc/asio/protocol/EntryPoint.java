package at.ac.univie.isc.asio.protocol;

import java.security.Principal;
import java.util.EnumSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.DatasetOperation.Action;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.metadata.DatasetMetadata;
import at.ac.univie.isc.asio.security.VphTokenExtractor;

import com.google.common.base.Supplier;

@Path("/{permission: (read|full) }")
public class EntryPoint {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(EntryPoint.class);

  // FIXME inject this
  private final VphTokenExtractor security = new VphTokenExtractor();

  @Context
  private Request request;
  @Context
  private HttpHeaders headers;

  private final EndpointSupplier endpoints;
  private final Supplier<DatasetMetadata> metadata;

  public EntryPoint(final EndpointSupplier endpoints, final Supplier<DatasetMetadata> metadata) {
    super();
    this.endpoints = endpoints;
    this.metadata = metadata;
  }

  @GET
  @Path("/meta")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public DatasetMetadata serveMetadata() {
    return metadata.get();
  }

  @Path("/{language: (sql|sparql) }")
  public Endpoint forward(@PathParam("permission") final String permission,
      @PathParam("language") final String language) {
    log.debug(">> handling {} request with permission {}", language, permission);
    final Language lang = Language.fromString(language);
    final Endpoint target = endpoints.get(lang);
    final Principal owner = security.authenticate(headers);
    final Set<Action> allowed = permissionsFor(permission);
    log.info("-- user : {}", owner);
    return target.inject(request, headers).authorize(owner, allowed);
  }

  private Set<Action> permissionsFor(final String permission) {
    switch (permission) {
      case "read":
        return EnumSet.of(Action.QUERY, Action.SCHEMA);
      case "full":
        return EnumSet.of(Action.QUERY, Action.UPDATE, Action.SCHEMA);
      default:
        throw new WebApplicationException(Response.status(Status.NOT_FOUND).build());
    }
  }
}
