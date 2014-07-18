package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.engine.sql.SqlSchema;
import at.ac.univie.isc.asio.security.Role;
import com.google.common.base.Supplier;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("/{permission}/meta")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public final class MetadataResource {

  private final Supplier<DatasetMetadata> metadata;
  private final Supplier<SqlSchema> schema;

  @Context
  private SecurityContext security;

  // required for declarative JAXRS config
  @Deprecated
  public MetadataResource() { throw new AssertionError("attempt to use non-managed resource"); }

  public MetadataResource(final Supplier<DatasetMetadata> metadata, final Supplier<SqlSchema> schema) {
    this.metadata = metadata;
    this.schema = schema;
  }

  @GET
  public DatasetMetadata metadata() {
    checkPermission();
    return metadata.get();
  }

  @Path("/schema")
  @GET
  public SqlSchema schema() {
    checkPermission();
    return schema.get();
  }

  private void checkPermission() {
    if (!security.isUserInRole(Role.READ.name())) {
      throw new ForbiddenException();
    }
  }
}
