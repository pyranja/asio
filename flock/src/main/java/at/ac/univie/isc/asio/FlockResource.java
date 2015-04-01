package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.security.IncludeRequestMethodSecurityContext;
import at.ac.univie.isc.asio.security.SecurityContextHolder;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

public class FlockResource {
  private final ProtocolResourceFactory protocolBuilder;
  private final ConnectorChain chain;

  @Deprecated
  public FlockResource() {
    throw new AssertionError("attempt to use non-managed resource");
  }

  public FlockResource(final ProtocolResourceFactory protocolBuilder, final ConnectorChain chain) {
    this.protocolBuilder = protocolBuilder;
    this.chain = chain;
  }

  @Path("/sparql")
  public ProtocolResource protocol(@Context Request request, @Context HttpHeaders headers,
                                   @Context SecurityContext security) {
    SecurityContextHolder.set(IncludeRequestMethodSecurityContext.wrap(security, request));
    final Connector connector = this.chain.create();
    return protocolBuilder.create(ParseJaxrsCommand.with(Language.SPARQL).withHeaders(headers).withOwner(security.getUserPrincipal()), connector);
  }
}
