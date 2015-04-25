package at.ac.univie.isc.asio.insight;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Priority;
import javax.inject.Provider;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

/**
 * Inject the request/response correlation id into the response headers.
 */
@Component
@javax.ws.rs.ext.Provider
@Priority(Priorities.HEADER_DECORATOR)
public final class InjectCorrelationFilter implements ContainerResponseFilter {
  public static final String CORRELATION_HEADER = "Correlation";

  private final Provider<Correlation> correlation;

  @Autowired
  InjectCorrelationFilter(final Provider<Correlation> provider) {
    this.correlation = provider;
  }

  @Override
  public void filter(final ContainerRequestContext request,
                     final ContainerResponseContext response) throws IOException {
    response.getHeaders().putSingle(CORRELATION_HEADER, correlation.get());
  }
}
