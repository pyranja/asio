package at.ac.univie.isc.asio;

import at.ac.univie.isc.asio.engine.*;
import at.ac.univie.isc.asio.security.IncludeRequestMethodSecurityContext;
import at.ac.univie.isc.asio.security.SecurityContextHolder;
import com.google.common.base.Supplier;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

public class FlockResource {
  private final ProtocolResourceFactory protocolBuilder;
  private final ConnectorChain chain;
  private final Supplier<EventReporter> scopedEvents;

  @Deprecated
  public FlockResource() {
    throw new AssertionError("attempt to use non-managed resource");
  }

  public FlockResource(final ProtocolResourceFactory protocolBuilder, final ConnectorChain chain, final Supplier<EventReporter> scopedEvents) {
    this.protocolBuilder = protocolBuilder;
    this.chain = chain;
    this.scopedEvents = scopedEvents;
  }

  @Path("/sparql")
  public ProtocolResource protocol(@Context Request request, @Context HttpHeaders headers,
                                   @Context SecurityContext security) {
    SecurityContextHolder.set(IncludeRequestMethodSecurityContext.wrap(security, request));
    final Connector connector = this.chain.create(scopedEvents.get());
    return protocolBuilder.create(ParseJaxrsCommand.with(Language.SPARQL).including(headers).initiatedBy(security.getUserPrincipal()), connector);
  }
}
