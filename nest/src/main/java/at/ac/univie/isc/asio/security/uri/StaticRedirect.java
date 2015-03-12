package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;

/**
 * This rule requires the component {@code tail} to be present in the
 * {@link UriAuthRule.PathElements parsed path} and the authority
 * is always set to {@link at.ac.univie.isc.asio.security.Role#NONE}.
 * <p>
 * Redirects paths containing a marker element to a prefixed base path, e.g. given the
 * marker {@code 'marker-element'} and redirect prefix {@code 'redirect-prefix'}:
 * </p>
 * <p>
 * {@code /some/path/marker-element/rest/of/path}  ==>  {@code /redirect-prefix/rest/of/path}
 * </p>
 */
public final class StaticRedirect implements UriAuthRule {
  /**
   * Pattern for default asio requests. A single head element designates the target schema or api
   * endpoint. Tail may contain static marker.
   */
  public static final String URI_WITH_SCHEMA_HEAD = "(?<head>/[^/]+)(?<tail>.*)?";

  /** key of inspected path in {@link UriAuthRule.PathElements} */
  public static final String PATH_GROUP_KEY = "tail";

  private final String marker;
  private final String prefix;

  private StaticRedirect(final String marker, final String redirectPrefix) {
    this.marker = marker;
    prefix = redirectPrefix;
  }

  public static StaticRedirect create(final String marker, final String redirectPrefix) {
    return new StaticRedirect(marker, redirectPrefix);
  }

  @Override
  public boolean canHandle(final PathElements pathElements) {
    return pathElements.require(PATH_GROUP_KEY).startsWith(marker);
  }

  @Override
  public FindAuthorization.AuthAndRedirect handle(final PathElements pathElements) {
    final String tail = pathElements.require(PATH_GROUP_KEY).substring(marker.length());
    final String redirect = prefix + tail;
    return FindAuthorization.AuthAndRedirect.create(FindAuthorization.AuthAndRedirect.NO_AUTHORITY, redirect);
  }
}
