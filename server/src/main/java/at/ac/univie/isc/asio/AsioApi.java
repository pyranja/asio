package at.ac.univie.isc.asio;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * rest api settings.
 */
public class AsioApi {
  /**
   * The name of the http request head, which should be inspected for delegated credentials.
   */
  @NotEmpty
  public String delegateAuthorizationHeader;

  /**
   * The name of the query parameter used to override the 'Accept' header.
   */
  @NotEmpty
  public String overrideAcceptParameter;

  /**
   * The default media type used when no 'Accept' header is present.
   */
  @NotNull
  public List<String> defaultMediaType;

  /**
   * The default language used when no 'Accept-Language' header is present.
   */
  @NotEmpty
  public String defaultLanguage;

  /**
   * Names that cannot be assigned to a container, e.g. because it would clash with another resource
   * path in the REST api.
   */
  @NotNull
  public List<Id> reservedContainerNames = new ArrayList<>();

  @Override
  public String toString() {
    return "{" +
        "delegateAuthorizationHeader='" + delegateAuthorizationHeader + '\'' +
        ", overrideAcceptParameter='" + overrideAcceptParameter + '\'' +
        ", defaultMediaType=" + defaultMediaType +
        ", defaultLanguage='" + defaultLanguage + '\'' +
        ", reservedContainerNames=" + reservedContainerNames +
        '}';
  }

  public String getDelegateAuthorizationHeader() {
    return delegateAuthorizationHeader;
  }

  public void setDelegateAuthorizationHeader(final String delegateAuthorizationHeader) {
    this.delegateAuthorizationHeader = delegateAuthorizationHeader;
  }

  public String getOverrideAcceptParameter() {
    return overrideAcceptParameter;
  }

  public void setOverrideAcceptParameter(final String overrideAcceptParameter) {
    this.overrideAcceptParameter = overrideAcceptParameter;
  }

  public List<String> getDefaultMediaType() {
    return defaultMediaType;
  }

  public void setDefaultMediaType(final List<String> defaultMediaType) {
    this.defaultMediaType = defaultMediaType;
  }

  public String getDefaultLanguage() {
    return defaultLanguage;
  }

  public void setDefaultLanguage(final String defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
  }

  public List<Id> getReservedContainerNames() {
    return reservedContainerNames;
  }

  public void setReservedContainerNames(final List<Id> reservedContainerNames) {
    this.reservedContainerNames = reservedContainerNames;
  }
}
