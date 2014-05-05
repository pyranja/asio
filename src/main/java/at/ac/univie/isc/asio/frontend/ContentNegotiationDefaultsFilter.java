package at.ac.univie.isc.asio.frontend;

import com.google.common.collect.Iterables;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Replace missing or empty Accept-* headers with default values.
 */
//FIXME @Priority(Priorities.USER)
public class ContentNegotiationDefaultsFilter implements ContainerRequestFilter {

  @Override
  public void filter(final ContainerRequestContext context) throws IOException {
    final MultivaluedMap<String, String> headers = context.getHeaders();
    if (noAcceptedTypeIn(headers) || containsNoConcreteType(headers.get(HttpHeaders.ACCEPT))) {
      headers.putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML);
    }
    if (!headers.containsKey(HttpHeaders.ACCEPT_LANGUAGE)) {
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
