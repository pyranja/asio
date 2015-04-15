package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.AsioError;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.insight.VndError;
import com.google.common.collect.ImmutableList;
import org.glassfish.jersey.media.sse.EventSource;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Client proxy to query and control asio servers.
 */
public /* final */ class Pigeon {
  private static final Logger log = getLogger(Pigeon.class);

  /**
   * Create a proxy targeting the given server base address.
   * Proper authentication mechanism <strong>must</strong> have been configured on the backing
   * client.
   */
  public static Pigeon connectTo(final WebTarget base) {
    log.info("connecting to server @ <{}>", base.getUri());
    return new Pigeon(base);
  }

  private static ServerCommunicationFailure wrap(final Throwable cause) {
    String message = cause == null ? "unknown cause" : cause.getMessage();
    if (cause instanceof WebApplicationException) {
      final Response response = ((WebApplicationException) cause).getResponse();
      try {
        final VndError error = response.readEntity(VndError.class);
        message = cause.getMessage() + '\n' + error.getMessage();
      } catch (Exception ignored) {}
    }
    return new ServerCommunicationFailure(message, cause);
  }

  /**
   * Thrown if a request to the asio server fails in an unexpected way for any reason.
   */
  public static final class ServerCommunicationFailure extends AsioError.Base {
    private ServerCommunicationFailure(final String message, final Throwable cause) {
      super(message, cause);
    }
  }


  private final WebTarget server;

  private Pigeon(final WebTarget base) {
    server = base;
  }

  /**
   * Query the server for its health status. If the server is not reachable or the request fails,
   * the status is {@link ServerStatus#DOWN}. Except for fatal errors, this method should never
   * throw an exception.
   */
  @Nonnull
  public ServerStatus health() {
    try {
      final Response response = server
          .path("explore/insight/health")
          .request(MediaType.APPLICATION_JSON_TYPE)
          .get();
      log.info("received health response {}", response);
      if (httpStatus(response) == Response.Status.OK) {
        final ServerStatus status = response.readEntity(ServerStatus.class);
        return status == null ? ServerStatus.DOWN : status;
      } else {
        return ServerStatus.DOWN;
      }
    } catch (final Exception error) {
      log.info("server is not reachable - {}", error.getMessage());
      return ServerStatus.DOWN;
    }
  }

  /**
   * Connect to the server's event endpoint.
   */
  @Nonnull
  public EventSource eventStream() {
    return EventSource.target(server.path("api/events"))
        .named("munin-event-trace")
        .reconnectingEvery(100, TimeUnit.MILLISECONDS)
        .build();
  }

  /**
   * Fetch the ids of all active containers. May return an empty collection if either no container
   * is deployed, or container management is not supported by the server.
   */
  @Nonnull
  public Collection<Id> activeContainer() {
    try {
      return server
          .path("api/container")
          .request(MediaType.APPLICATION_JSON_TYPE)
          .get(new ContainerIdList());
    } catch (NotFoundException notFound) {
      log.debug("container management not supported");
      return ImmutableList.of();
    } catch (ProcessingException | WebApplicationException e) {
      throw wrap(e);
    }
  }

  /**
   * Create a new container from the given raw mapping data.
   */
  @Nonnull
  public Map<String, Object> deploy(final Id target, final byte[] mapping) {
    final Map<String, Object> deployed;
    try {
      deployed = server
          .path("api/container/{id}")
          .resolveTemplate("id", target)
          .request(MediaType.APPLICATION_JSON_TYPE)
          .put(Entity.entity(mapping, MediaType.valueOf("text/turtle")), new JsonMap());
      log.debug("container {} deployed successfully");
      return deployed == null ? Collections.<String, Object>emptyMap() : deployed;
    } catch (ProcessingException | WebApplicationException e) {
      throw wrap(e);
    }
  }

  /**
   * If present, undeploy and destroy the container with given id.
   *
   * @return true if a container was destroyed, false if none was found
   */
  @Nonnull
  public boolean undeploy(final Id target) {
    try {
      server
          .path("api/container/{id}")
          .resolveTemplate("id", target)
          .request(MediaType.APPLICATION_JSON_TYPE)
          .delete(Map.class);
      log.debug("container {} undeployed successfully");
      return true;
    } catch (NotFoundException ignore) {
      log.debug("container {} not found on undeploy", target);
      return false;
    } catch (ProcessingException | WebApplicationException e) {
      throw wrap(e);
    }
  }

  private static class ContainerIdList extends GenericType<List<Id>> {
    /* type token */
  }

  private static class JsonMap extends GenericType<Map<String, Object>> {
    /* type token */
  }

  private Response.Status httpStatus(final Response response) {
    return Response.Status.fromStatusCode(response.getStatus());
  }
}
