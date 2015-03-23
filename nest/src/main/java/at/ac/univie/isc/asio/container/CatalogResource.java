package at.ac.univie.isc.asio.container;

import at.ac.univie.isc.asio.Schema;
import at.ac.univie.isc.asio.d2rq.D2rqContainerAdapter;
import at.ac.univie.isc.asio.security.AuthInfo;
import at.ac.univie.isc.asio.security.Identity;
import com.google.common.io.Files;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Collection;

@Component
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {
  private final ContainerDirector director;
  private final SettingsRegistry settings;
  private final SecurityContext security;

  @Autowired
  public CatalogResource(final ContainerDirector director,
                         final SettingsRegistry settings,
                         final SecurityContext security) {
    this.director = director;
    this.settings = settings;
    this.security = security;
  }

  @GET
  @Path("/whoami")
  public AuthInfo getClientInfo() {
    final Authentication authentication = security.getAuthentication();
    final Identity identity = authentication.getCredentials() instanceof Identity
        ? (Identity) authentication.getCredentials()
        : Identity.undefined();
    return AuthInfo.from(authentication.getName(), identity, authentication.getAuthorities());
  }

  @GET
  @Path("/catalog")
  public Collection<Schema> allDeployed() {
    return settings.findAll();
  }

  @GET
  @Path("/catalog/{schema}")
  public ContainerSettings get(@PathParam("schema") final Schema schema) {
    return settings.settingsOf(schema);
  }

  @PUT
  @Path("/catalog/{schema}")
  @Consumes("text/turtle")
  public Response deployFromD2r(@PathParam("schema") final Schema schema, final File mapping) {
    final D2rqContainerAdapter adapter = D2rqContainerAdapter.load(Files.asByteSource(mapping));
    director.createNewOrReplace(schema, adapter);
    return Response.status(Response.Status.CREATED).build();
  }

  @PUT
  @Path("/catalog/{schema}")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deploy(@PathParam("schema") final Schema schema, final ContainerSettings settings) {
    final DefaultsAdapter adapter = DefaultsAdapter.from(settings);
    director.createNewOrReplace(schema, adapter);
    return Response.status(Response.Status.CREATED).build();
  }

  @DELETE
  @Path("/catalog/{schema}")
  public void drop(@PathParam("schema") final Schema schema) {
    final ContainerDirector.Result outcome = director.dispose(schema);
    if (outcome == ContainerDirector.Result.NOOP) {
      throw new NotFoundException(schema.name() + " not found");
    }
  }
}
