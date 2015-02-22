package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Scope;
import com.google.common.eventbus.Subscribe;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Publish events from the internal event bus as server-sent-event streams.
 */
@Component
@Path("/events")
public class EventResource implements AutoCloseable {
  private static final Logger log = getLogger(EventResource.class);

  /** sent immediately on registration */
  static final OutboundEvent SUBSCRIBED = new OutboundEvent.Builder()
      .name("stream").data(String.class, "subscribed").mediaType(MediaType.TEXT_PLAIN_TYPE).build();

  private final SseBroadcaster broadcaster = new SseBroadcaster();

  @GET
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  @PreAuthorize("hasAuthority('PERMISSION_ACCESS_INTERNALS')")
  public EventOutput subscribe(@Context final SecurityContext security) throws IOException {
    final EventOutput subscription = new EventOutput();
    subscription.write(SUBSCRIBED);
    broadcaster.add(subscription);
    log.debug(Scope.SYSTEM.marker(), "{} subscribed to event stream", security.getUserPrincipal());
    return subscription;
  }

  @Subscribe
  public void publish(final Event event) {
    final OutboundEvent sse = new OutboundEvent.Builder()
        .name(event.type())
        .data(String.class, event.data())
        .mediaType(MediaType.TEXT_PLAIN_TYPE)
        .build();
    broadcaster.broadcast(sse);
  }

  @DELETE
  @PreDestroy
  @PreAuthorize("hasAuthority('PERMISSION_ADMINISTRATE')")
  @Override
  public void close() throws Exception {
    log.info(Scope.SYSTEM.marker(), "closing all event streams");
    broadcaster.closeAll();
  }
}
