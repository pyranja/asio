package at.ac.univie.isc.asio.jaxrs;

import at.ac.univie.isc.asio.Scope;
import com.google.common.base.Predicate;
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
public final class ContentNegotiationDefaultsFilter implements ContainerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(ContentNegotiationDefaultsFilter.class);

  private final String defaultLanguage;
  private final List<String> defaultMediaType;

  public ContentNegotiationDefaultsFilter(final List<String> mediaType, final String language) {
    this.defaultLanguage = language;
    defaultMediaType = mediaType;
    log.info(Scope.SYSTEM.marker(), "initialize defaults filter with media types '{}' and language '{}'",
        defaultMediaType, defaultLanguage);
  }

  @Override
  public void filter(final ContainerRequestContext context) throws IOException {
    final MultivaluedMap<String, String> headers = context.getHeaders();
    if (headerNotDefined(HttpHeaders.ACCEPT, headers)
        || containsNoConcreteType(headers.get(HttpHeaders.ACCEPT))) {
      log.debug(Scope.REQUEST.marker(), "header {Accept} set to default '{}'", defaultMediaType);
      headers.put(HttpHeaders.ACCEPT, defaultMediaType);
    }
    if (headerNotDefined(HttpHeaders.ACCEPT_LANGUAGE, headers)) {
      log.debug(Scope.REQUEST.marker(), "header {Accept-Language} set to default '{}'", Locale.ENGLISH.getLanguage());
      headers.putSingle(HttpHeaders.ACCEPT_LANGUAGE, defaultLanguage);
    }
  }

  private boolean containsNoConcreteType(final List<String> accepted) {
    return Iterables.all(accepted, new Predicate<String>() {
      @Override
      public boolean apply(final String input) {
        return input == null || input.equals(MediaType.WILDCARD);
      }
    });
  }

  private boolean headerNotDefined(final String headerName, final MultivaluedMap<String, String> headers) {
    return !headers.containsKey(headerName) || headers.get(headerName).isEmpty();
  }
}
