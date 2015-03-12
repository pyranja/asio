package at.ac.univie.isc.asio.security.uri;

import at.ac.univie.isc.asio.security.FindAuthorization;

/**
 * Always handles without redirect and no authority.
 */
public final class NoopRule implements UriAuthRule {
  private static final NoopRule INSTANCE = new NoopRule();

  public static NoopRule instance() {
    return INSTANCE;
  }

  private NoopRule() {}

  @Override
  public boolean canHandle(final PathElements pathElements) {
    return true;
  }

  @Override
  public FindAuthorization.AuthAndRedirect handle(final PathElements pathElements) {
    return FindAuthorization.AuthAndRedirect.noRedirect(FindAuthorization.AuthAndRedirect.NO_AUTHORITY);
  }
}
