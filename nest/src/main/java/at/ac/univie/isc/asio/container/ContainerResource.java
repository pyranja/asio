package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.Scope;
import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ContainerResource implements AutoCloseable {
  private static final Logger log = getLogger(ContainerResource.class);

  private final Catalog<Container> catalog;
  private final ConfigStore config;
  private final Assembler assembler;

  @Autowired
  public ContainerResource(final Catalog<Container> catalog,
                           final ConfigStore config,
                           final Assembler assembler) {
    this.catalog = catalog;
    this.config = config;
    this.assembler = assembler;
  }

  @GET
  public Collection<Schema> listContainers() {
    return catalog.findKeys();
  }

  @GET
  @Path("/{id}")
  public Response findContainer(@PathParam("id") final Schema schema) {
    final Optional<Container> container = catalog.find(schema);
    return container.isPresent()
        ? Response.ok(container.get()).build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  @PUT
  @Path("/{id}")
  @Consumes("text/turtle")
  public Response createD2rqContainer(@PathParam("id") final Schema target, final File upload) {
    final ByteSource source = Files.asByteSource(upload);
    final Container container = assembler.assemble(target, source);
    deploy(container, source);
    return Response.status(Response.Status.CREATED).build();
  }

  @DELETE
  @Path("/{id}")
  public Response deleteContainer(@PathParam("id") final Schema target) {
    final boolean wasPresent = dispose(target);
    return wasPresent
        ? Response.ok().build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }

  // === internal - visible for testing purposes ===================================================

  /**
   * Create and deploy a container created from the given assembler.
   *
   * @param container     container that will be deployed
   * @param configuration raw configuration data of the deployed container
   */
  void deploy(final Container container, final ByteSource configuration) {
    final Schema target = container.name();
    log.debug(Scope.SYSTEM.marker(), "create container <{}>", target);
    catalog.atomic(target, new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        dispose(target);
        final URI location =
            ContainerResource.this.config.save(target.name(), "config", configuration);
        log.debug(Scope.SYSTEM.marker(), "saved configuration at <{}>", location);
        container.activate();
        log.debug(Scope.SYSTEM.marker(), "activated {} as <{}>", container, target);
        final Optional<Container> replaced = catalog.deploy(container);
        assert !replaced.isPresent() : "container was present on deploying of " + target;
        return null;
      }
    });
  }

  /**
   * If present undeploy and dispose the container with given name.
   *
   * @param target name of target container
   * @return true if the target container was present and has been dropped, false if not present
   */
  boolean dispose(final Schema target) {
    log.debug(Scope.SYSTEM.marker(), "dispose container <{}>", target);
    return catalog.atomic(target, new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final Optional<Container> dropped = catalog.drop(target);
        if (dropped.isPresent()) {
          final Container container = dropped.get();
          log.debug(Scope.SYSTEM.marker(), "found {} for <{}> - destroying it", container, target);
          container.close();
        }
        config.clear(target.name());
        return dropped.isPresent();
      }
    });
  }

  /**
   * Clear the backing catalog and drop all remaining containers.
   */
  @PreDestroy
  @Override
  public void close() {
    log.info(Scope.SYSTEM.marker(), "shutting down");
    final Set<Container> remaining = catalog.clear();
    for (final Container container : remaining) {
      try {
        log.debug(Scope.SYSTEM.marker(), "closing {}", container);
        container.close();
      } catch (Exception e) {
        log.error(Scope.SYSTEM.marker(), "failed to close a container", e);
      }
    }
  }
}
