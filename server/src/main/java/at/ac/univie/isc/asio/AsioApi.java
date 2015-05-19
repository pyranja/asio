/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
