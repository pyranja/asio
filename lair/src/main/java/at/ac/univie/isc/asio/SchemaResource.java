package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.security.IncludeRequestMethodSecurityContext;
import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.security.SecurityContextHolder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.*;
import java.net.URI;

/**
 * Entry point to a single dataset.
 */
public class SchemaResource {
  private final ProtocolResourceFactory protocolBuilder;
  private final ConnectorChain chain;

  @Deprecated
  public SchemaResource() {
    throw new AssertionError("attempt to use non-managed resource");
  }

  public SchemaResource(final ProtocolResourceFactory protocolBuilder, final ConnectorChain chain) {
    this.protocolBuilder = protocolBuilder;
    this.chain = chain;
  }

  @Path("/sql/schema")
  @GET
  @Deprecated // use MetadataResource#schema directly.
  public Response serveSchema(@Context final UriInfo uri) {
    final URI redirect = uri.getBaseUriBuilder().path(Permission.READ.name()).path("meta/schema").build();
    return Response.status(Response.Status.MOVED_PERMANENTLY)
        .header(HttpHeaders.LOCATION, redirect).build();
  }

  @Path("/{language}")
  public ProtocolResource protocol(@PathParam("language") final Language language,
                                   @Context Request request,
                                   @Context HttpHeaders headers,
                                   @Context SecurityContext security) {
    SecurityContextHolder.set(IncludeRequestMethodSecurityContext.wrap(security, request));
    final Connector connector = this.chain.create();
    return protocolBuilder.create(ParseJaxrsCommand.with(language).including(headers).initiatedBy(security.getUserPrincipal()), connector);
  }
}
