package at.ac.univie.isc.asio.frontend;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports tunneling of accept-header through query parameters.
 * 
 * @author Chris Borckholder
 */
@Provider
public class AcceptTunnelFilter implements ContainerRequestFilter {

  /* slf4j-logger */
  private final static Logger log = LoggerFactory.getLogger(AcceptTunnelFilter.class);

  public static final String ACCEPT_PARAM_TUNNEL = "x-asio-accept";

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    final UriInfo uri = requestContext.getUriInfo();
    final MultivaluedMap<String, String> params = uri.getQueryParameters();
    if (params.containsKey(ACCEPT_PARAM_TUNNEL)) {
      final String accept = params.getFirst(ACCEPT_PARAM_TUNNEL);
      log.debug("replacing accept header with tunneled parameter {}", accept);
      requestContext.getHeaders().putSingle(HttpHeaders.ACCEPT, accept);
      requestContext.getHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, "en");
    }
  }
}
