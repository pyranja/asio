package at.ac.univie.isc.asio.metadata;

import at.ac.univie.isc.asio.SchemaIdentifier;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.metadata.sql.RelationalSchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provide Metadata on a single deployed schema.
 */
@Component
@Path("/{schema}")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@PreAuthorize("hasAuthority('PERMISSION_ACCESS_METADATA')")
public class MetaResource {
  private final MetadataService metadataService;
  private final RelationalSchemaService schemaService;

  @Autowired
  public MetaResource(final MetadataService metadataService, final RelationalSchemaService schemaService) {
    this.metadataService = metadataService;
    this.schemaService = schemaService;
  }

  @GET
  @Path("/meta")
  public DatasetMetadata fetchDatasetMetadata() {
    return StaticMetadata.NOT_AVAILABLE;
  }

//  @GET
//  @Path("/meta")
//  public DatasetDescription fetchDatasetDescriptor() {
//    return metadataService.fetchDescriptor();
//  }

  @GET
  @Path("/schema")
  public SqlSchema fetchRelationalSchema(@PathParam("schema") final SchemaIdentifier identifier) {
    return schemaService.explore(identifier);
  }

  @GET
  @Path("/meta/schema")
  @Deprecated
  public SqlSchema legacySchemaPath(@PathParam("schema") final SchemaIdentifier identifier) {
    return fetchRelationalSchema(identifier);
  }
}