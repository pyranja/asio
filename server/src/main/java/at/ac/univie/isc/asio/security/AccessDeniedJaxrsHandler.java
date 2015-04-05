package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.insight.Correlation;
import at.ac.univie.isc.asio.insight.Error;
import at.ac.univie.isc.asio.platform.CurrentTime;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public final class AccessDeniedJaxrsHandler implements ExceptionMapper<AccessDeniedException> {

  // TODO : inject correctly scoped
  private static final Correlation FIXED_PLACEHOLDER = Correlation.valueOf("none");

  @Override
  public Response toResponse(final AccessDeniedException exception) {
    final Error error = Error.from(exception, FIXED_PLACEHOLDER, CurrentTime.instance().read(), false);
    return Response.status(Response.Status.FORBIDDEN)
        .type(MediaType.APPLICATION_JSON_TYPE).entity(error)
        .build();
  }
}
