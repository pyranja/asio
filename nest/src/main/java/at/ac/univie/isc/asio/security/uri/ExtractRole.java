package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Extract the role name embedded in the URI and remove it from the URI in the redirection.
 * Requires the matched groups {@code head}, {@code tail} and {@code authority} to be present.
 */
public final class ExtractRole implements UriAuthRule {
  /*
   * Regular expression for an URI with embedded role name.
   */
  public static final String URI_WITH_ROLE_REGEX =
      "(?<head>/[^/]+)/(?<authority>[^/?#]+)(?<tail>.*)";

  private static final ExtractRole INSTANCE = new ExtractRole();

  public static ExtractRole instance() {
    return INSTANCE;
  }

  private ExtractRole() {}

  @Override
  public boolean canHandle(final PathElements pathElements) {
    return true;
  }

  @Override
  public FindAuthorization.AuthAndRedirect handle(final PathElements pathElements) {
    final SimpleGrantedAuthority authority =
        new SimpleGrantedAuthority(pathElements.require("authority"));
    final String redirect = pathElements.require("head") + pathElements.require("tail");
    return FindAuthorization.AuthAndRedirect.create(authority, redirect);
  }
}
