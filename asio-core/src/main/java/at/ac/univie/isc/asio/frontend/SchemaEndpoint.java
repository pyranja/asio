package at.ac.univie.isc.asio.frontend;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.isc.asio.coordination.EngineSpec.Type;
import at.ac.univie.isc.asio.frontend.OperationFactory.OperationBuilder;

/**
 * Endpoint that serves the schema of a Dataset, e.g. the relational schema.
 * 
 * @author Chris Borckholder
 */
@Path("/schema/")
public final class SchemaEndpoint extends AbstractEndpoint {

  /* slf4j-logger */
  final static Logger log = LoggerFactory.getLogger(SchemaEndpoint.class);

  public SchemaEndpoint(final EngineSelector registry, final AsyncProcessor processor,
      final OperationFactory create, final Type type) {
    super(registry, processor, create, type);
    // TODO Auto-generated constructor stub
  }

  /**
   * Delivers the schema of this DatasetEngine.
   * 
   * @return schema of this dataset
   */
  @GET
  public void serveSchema(@Context final Request request, @Suspended final AsyncResponse response) {
    log.debug("-- serving schema");
    final OperationBuilder partial = create.schema();
    complete(partial, request, response);
  }
}
