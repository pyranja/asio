package at.ac.univie.isc.asio.web;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helpers for working with URIs.
 */
public final class WebTools {

  /**
   * Ensure the given URI ends with a {@code '/'}, i.e. it points to the root of a directory,
   * not to a document. If no path is defined, it is set to the root path ({@code '/'}).
   *
   * @return URI where the path ends with '/'
   */
  public static URI ensureDirectoryPath(final URI it) {
    final String path = it.getPath();
    if (isDocumentPath(path)) {
      return cloneWithPath(it, path + "/");
    }
    return it;
  }

  private static boolean isDocumentPath(final String path) {
    return path != null && !path.endsWith("/");
  }

  private static URI cloneWithPath(final URI it, final String path) {
    try {
      return new URI(it.getScheme(), it.getUserInfo(), it.getHost(), it.getPort(), path, it.getQuery(), it.getFragment());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

}
