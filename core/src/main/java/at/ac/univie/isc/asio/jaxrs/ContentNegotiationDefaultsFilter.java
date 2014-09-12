package at.ac.univie.isc.asio.jaxrs;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Replace missing or empty Accept-* headers with default values.
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class ContentNegotiationDefaultsFilter implements ContainerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(ContentNegotiationDefaultsFilter.class);

  @Override
  public void filter(final ContainerRequestContext context) throws IOException {
    final MultivaluedMap<String, String> headers = context.getHeaders();
    if (noAcceptedTypeIn(headers) || containsNoConcreteType(headers.get(HttpHeaders.ACCEPT))) {
      log.debug("header {Accept} set to default '{}'", MediaType.APPLICATION_XML);
      headers.putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
    }
    if (!headers.containsKey(HttpHeaders.ACCEPT_LANGUAGE)) {
      log.debug("header {Accept-Language} set to default '{}'", Locale.ENGLISH.getLanguage());
      headers.putSingle(HttpHeaders.ACCEPT_LANGUAGE, Locale.ENGLISH.getLanguage());
    }
  }

  private boolean containsNoConcreteType(final List<String> accepted) {
    if (accepted.size() != 1) {
      return false;
    }
    final MediaType present = MediaType.valueOf(Iterables.getOnlyElement(accepted));
    return MediaType.WILDCARD_TYPE.equals(present);
  }

  private boolean noAcceptedTypeIn(final MultivaluedMap<String, String> headers) {
    return !headers.containsKey(HttpHeaders.ACCEPT) || headers.get(HttpHeaders.ACCEPT).isEmpty();
  }
}
