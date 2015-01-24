package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.*;
import com.google.common.base.Supplier;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

public class FlockResource {
  private final ProtocolResourceFactory protocolBuilder;
  private final AllInOneConnectorFactory connectorBuilder;
  private final Supplier<EventReporter> scopedEvents;

  @Deprecated
  public FlockResource() {
    throw new AssertionError("attempt to use non-managed resource");
  }

  public FlockResource(final ProtocolResourceFactory protocolBuilder, final AllInOneConnectorFactory connectorBuilder, final Supplier<EventReporter> scopedEvents) {
    this.protocolBuilder = protocolBuilder;
    this.connectorBuilder = connectorBuilder;
    this.scopedEvents = scopedEvents;
  }

  @Path("/sparql")
  public ProtocolResource protocol(@Context Request request, @Context HttpHeaders headers,
                                   @Context SecurityContext security) {
    final IsAuthorized authorizer = IsAuthorized.given(security, request);
    final AllInOneConnector connector = this.connectorBuilder.create(authorizer, scopedEvents.get());
    return protocolBuilder.create(ParseJaxrsParameters.with(Language.SPARQL).including(headers).initiatedBy(security.getUserPrincipal()), connector);
  }
}
