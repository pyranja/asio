package at.ac.univie.isc.asio.brood;

import at.ac.univie.isc.asio.Brood;
import at.ac.univie.isc.asio.Container;
import at.ac.univie.isc.asio.Id;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Collection;

@Brood
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ContainerResource extends BaseContainerRegistry {
  private final Warden warden;

  @Autowired
  public ContainerResource(final Warden warden) {
    this.warden = warden;
  }

  @GET
  public Collection<Id> listContainers() {
    return registry.keySet();
  }

  @GET
  @Path("/{id}")
  public Container findContainer(@PathParam("id") final Id id) {
    return find(id);
  }

  @PUT
  @Path("/{id}")
  @Consumes("text/turtle")
  public Response createD2rqContainer(@PathParam("id") final Id target, final File upload) {
    final ByteSource source = Files.asByteSource(upload);
    warden.assembleAndDeploy(target, source);
    return Response.status(Response.Status.CREATED).build();
  }

  @DELETE
  @Path("/{id}")
  public Response deleteContainer(@PathParam("id") final Id target) {
    final boolean wasPresent = warden.dispose(target);
    return wasPresent
        ? Response.ok().build()
        : Response.status(Response.Status.NOT_FOUND).build();
  }
}
