package at.ac.univie.isc.asio.insight;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.BroadcasterListener;
import org.glassfish.jersey.server.ChunkedOutput;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Echo all POST requests using server-sent-events to the subscribed listeners.
 */
@Singleton
@Path("/")
public final class SseEchoServer {
  private static final Logger log = getLogger(SseEchoServer.class);

  /** port will be bound to any free one dynamically */
  public static final int DEFAULT_PORT = 0;

  /**
   * Run the echo server in a standalone jersey container. Accepts a single argument, to override
   * the http port to use.
   */
  public static void main(String... args) {
    final int port = args.length > 0 ? Integer.valueOf(args[0]) : DEFAULT_PORT;
    final URI address = URI.create("http://localhost:" + port + "/");
    log.info("starting sse-echo-server @ <{}>", address);
    final HttpServer server = start(address);
    final URI actual = URI.create("http://localhost:" + findPort(server));
    try {
      log.info("sse-echo-server running @ <{}> (Ctrl-C to break) ...", actual);
      for (;;);
    } finally {
      log.info("shutting down sse-echo-server");
      server.shutdownNow();
    }
  }

  /** create and start the server at given network interface */
  public static HttpServer start(final URI address) {
    final ResourceConfig config = new ResourceConfig(SseEchoServer.class);
    return GrizzlyHttpServerFactory.createHttpServer(address, config);
  }

  /** inspect a grizzly server to find the actual port it's bound to */
  public static int findPort(final HttpServer server) {
    final Iterator<NetworkListener> iterator = server.getListeners().iterator();
    final NetworkListener listener = iterator.next();
    assert !iterator.hasNext() : "found multiple network listeners - unable to determine http port";
    final int port = listener.getPort();
    assert port > 0 : "found zero or negative port - maybe server not yet started?";
    return port;
  }

  // === echo server implementation ================================================================

  private final SseBroadcaster broadcaster;
  private final AtomicInteger counter;

  public SseEchoServer() {
    counter = new AtomicInteger(0);
    broadcaster = new SseBroadcaster();
    broadcaster.add(new LogListener());
    broadcaster.add(new SubscribedCounter<OutboundEvent>(counter));
  }

  @GET
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  public EventOutput listen() {
    final EventOutput listener = new EventOutput();
    broadcaster.add(listener);
    counter.incrementAndGet();
    log.info("listener subscribed");
    return listener;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Integer listenerCount() {
    return counter.get();
  }

  @POST
  public String echo(final String message) {
    log.info("received '{}'", message);
    final OutboundEvent event = new OutboundEvent.Builder()
        .data(String.class, message)
        .mediaType(MediaType.TEXT_PLAIN_TYPE)
        .build();
    broadcaster.broadcast(event);
    log.info("message published");
    return message;
  }

  @DELETE
  public void close() {
    log.info("closing all connections");
    broadcaster.closeAll();
  }

  private static final class SubscribedCounter<ANY> implements BroadcasterListener<ANY> {
    private final AtomicInteger count;

    private SubscribedCounter(final AtomicInteger count) {
      this.count = count;
    }

    @Override
    public void onClose(final ChunkedOutput<ANY> ignored) {
      count.getAndDecrement();
    }

    @Override
    public void onException(final ChunkedOutput<ANY> ignored, final Exception error) {}
  }

  private static final class LogListener implements BroadcasterListener<OutboundEvent> {
    @Override
    public void onException(final ChunkedOutput<OutboundEvent> ignored, final Exception exception) {
      log.error("error while broadcasting server-sent-event", exception);
    }

    @Override
    public void onClose(final ChunkedOutput<OutboundEvent> ignored) {
      log.info("listener closed connection");
    }
  }
}
