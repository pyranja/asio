package at.ac.univie.isc.asio.security;

import at.ac.univie.isc.asio.jaxrs.Error;
import com.google.common.base.Throwables;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public final class AccessDeniedJaxrsHandler implements ExceptionMapper<AccessDeniedException> {

  @Override
  public Response toResponse(final AccessDeniedException exception) {
    final Error info = new Error();
    info.setMessage(exception.getMessage());
    info.setCause(exception.getClass().getSimpleName());
    info.setTrace(Throwables.getStackTraceAsString(exception));
    return Response.status(Response.Status.FORBIDDEN)
        .type(MediaType.APPLICATION_JSON_TYPE).entity(info)
        .build();
  }
}
