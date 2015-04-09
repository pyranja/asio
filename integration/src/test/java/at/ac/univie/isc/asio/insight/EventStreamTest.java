package at.ac.univie.isc.asio.insight;

import at.ac.univie.isc.asio.Unchecked;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.junit.*;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static at.ac.univie.isc.asio.insight.Events.payload;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class EventStreamTest {
  private static HttpServer server;
  private static Client client;
  private static URI echoServerAddress;
  private Iterable<InboundEvent> received;

  @BeforeClass
  public static void startEchoServer() {
    server = SseEchoServer.start(URI.create("http://localhost:0"));
    client = ClientBuilder.newClient();
    final int port = SseEchoServer.findPort(server);
    echoServerAddress = URI.create("http://localhost:" + port);
  }

  @AfterClass
  public static void stopEchoServer() {
    server.shutdownNow();
  }

  @AfterClass
  public static void closeClient() {
    client.close();
  }

  private WebTarget echoServer;
  private ConnectableObservable<InboundEvent> events;
  private Subscription connection;

  @Before
  public void setUp() throws Exception {
    echoServer = client.target(echoServerAddress);
    events = EventStream.listenTo(EventSource
        .target(echoServer)
        .reconnectingEvery(25, TimeUnit.MILLISECONDS)
        .build())
        .publish();
    connection = events.connect();
  }

  @After
  public void tearDown() {
    connection.unsubscribe();
    echoServer.request().delete();
  }

  @Test
  public void should_yield_events_from_the_server_event_stream() throws Exception {
    received = EventStream.collectAll(events.take(1));
    echoServer.request().post(Entity.text("test-message"));
    assertThat(received, contains(payload(equalTo("test-message"))));
  }

  @Test
  public void should_reconnect_automatically() throws Exception {
    received = EventStream.collectAll(events.take(2));
    echoServer.request().post(Entity.text("first-message"));
    echoServer.request().delete();  // should drop all connections on server side
    Unchecked.sleep(100, TimeUnit.MILLISECONDS);
    echoServer.request().post(Entity.text("second-message"));
    assertThat(received, contains(payload(equalTo("first-message")), payload(equalTo("second-message"))));
  }

  @Test
  public void should_accept_already_opened_event_source() throws Exception {
    EventStream.listenTo(EventSource.target(echoServer).open()).subscribe().unsubscribe();
  }

  @Test
  public void should_close_connection_after_unsubscribing() throws Exception {
    events.subscribe();
    final Integer before = echoServer.request(MediaType.TEXT_PLAIN).get(Integer.class);
    assertThat(before, equalTo(1));
    connection.unsubscribe();
    echoServer.request().delete();  // trigger server sided connection cleanup
    final Integer after = echoServer.request(MediaType.TEXT_PLAIN).get(Integer.class);
    assertThat(after, equalTo(0));
  }
}
