package at.ac.univie.isc.asio.metadata;

import com.google.common.base.Supplier;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/{permission}/meta")
public final class MetadataResource {

  private final Supplier<DatasetMetadata> metadata;

  @Context
  private SecurityContext security;

  // required for declarative JAXRS config
  @Deprecated
  public MetadataResource() { throw new AssertionError("attempt to use non-managed resource"); }

  public MetadataResource(final Supplier<DatasetMetadata> metadata) {
    this.metadata = metadata;
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public DatasetMetadata metadata() {
    if (security.isUserInRole("read")) {
      return metadata.get();
    } else {  // FIXME : use annotation based access control
      throw new ForbiddenException();
    }
  }
}
