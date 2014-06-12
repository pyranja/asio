package at.ac.univie.isc.asio.jaxrs;

import org.slf4j.MDC;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import at.ac.univie.isc.asio.tool.LogContext;

import static java.lang.String.valueOf;

/**
 * Injects request attributes into the {@link org.slf4j.MDC Message Diagnostics Context} of slf4j.
 *
 * @author Chris Borckholder
 */
@Provider
@Priority(10) // first in filter chain
public class LogContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Override
  public void filter(final ContainerRequestContext requestContext) throws IOException {
    // String.valueOf() is null safe
    MDC.put(LogContext.KEY_METHOD, valueOf(requestContext.getMethod()));
    final UriInfo uriInfo = requestContext.getUriInfo();
    MDC.put(LogContext.KEY_URI, valueOf(uriInfo.getRequestUri()));
    MDC.put(LogContext.KEY_PATH, valueOf(uriInfo.getPath()));
    MDC.put(LogContext.KEY_PARAMS, valueOf(uriInfo.getQueryParameters()));
    // null operation value
    MDC.put(LogContext.KEY_OP, LogContext.NULL_OPERATION);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext,
      final ContainerResponseContext responseContext) throws IOException {
    MDC.clear();
  }
}
