package at.ac.univie.isc.asio;

/**
 * All feature toggles.
 */
public class AsioFeatures {
  public static final String VPH_METADATA = "asio.feature.vphMetadata";
  public static final String VPH_URI_AUTH = "asio.feature.vphUriAuth";
  public static final String ALLOW_FEDERATION = "asio.feature.allowFederation";

  /**
   * Enable metadata lookup in the vph metadata repository. If enabled, the repository http endpoint
   * may be configured by setting the {@code asio.metadata-repository} property.
   */
  public boolean vphMetadata = false;

  /**
   * Enable authorization from role name embedded in request URI.
   * <p>
   * <strong>WARNING</strong>: If this is enabled, the server is unprotected unless external
   * authentication is provided, e.g. a proxy server.
   * </p>
   */
  public boolean vphUriAuth = false;

  /**
   * Allow SPARQL basic federated queries. If this feature is enabled, individual containers may
   * explicitly enable federated query support. If the feature is disabled, container settings are
   * overridden and no federated queries are accepted.
   */
  public boolean allowFederation = false;

  @Override
  public String toString() {
    return "{" +
        "vphMetadata=" + vphMetadata +
        ", vphUriAuth=" + vphUriAuth +
        ", allowFederation=" + allowFederation +
        '}';
  }

  public boolean isVphUriAuth() {
    return vphUriAuth;
  }

  public void setVphUriAuth(final boolean vphUriAuth) {
    this.vphUriAuth = vphUriAuth;
  }

  public boolean isVphMetadata() {
    return vphMetadata;
  }

  public void setVphMetadata(final boolean vphMetadata) {
    this.vphMetadata = vphMetadata;
  }

  public boolean isAllowFederation() {
    return allowFederation;
  }

  public void setAllowFederation(final boolean allowFederation) {
    this.allowFederation = allowFederation;
  }
}
