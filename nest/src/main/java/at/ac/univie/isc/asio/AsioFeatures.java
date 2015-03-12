package at.ac.univie.isc.asio;

/**
 * All feature toggles.
 */
public class AsioFeatures {
  /**
   * Enable authorization from role name embedded in request URI.
   * <p>
   * <strong>WARNING</strong>: If this is enabled, the server is unprotected unless external
   * authentication is provided, e.g. a proxy server.
   * </p>
   */
  public boolean vphUriAuth = false;

  /**
   * Automatically create a user account for each {@link at.ac.univie.isc.asio.security.Role},
   * with name equal to the role name.
   */
  public boolean simpleAuth = false;

  @Override
  public String toString() {
    return "AsioFeatures{" +
        "vphUriAuth=" + vphUriAuth +
        ", simpleAuth=" + simpleAuth +
        '}';
  }

  public boolean isVphUriAuth() {
    return vphUriAuth;
  }

  public void setVphUriAuth(final boolean vphUriAuth) {
    this.vphUriAuth = vphUriAuth;
  }

  public boolean isSimpleAuth() {
    return simpleAuth;
  }

  public void setSimpleAuth(final boolean simpleAuth) {
    this.simpleAuth = simpleAuth;
  }
}
