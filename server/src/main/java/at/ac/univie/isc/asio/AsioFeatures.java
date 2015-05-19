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

/**
 * All feature toggles.
 */
public class AsioFeatures {
  public static final String VPH_METADATA = "asio.feature.vphMetadata";
  public static final String VPH_URI_AUTH = "asio.feature.vphUriAuth";
  public static final String ALLOW_FEDERATION = "asio.feature.allowFederation";
  public static final String GLOBAL_DATASOURCE = "asio.feature.globalDatasource";
  public static final String MULTI_TENANCY = "asio.feature.multiTenancy";

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

  /**
   * Enable global configuration of the database connection. Implies, that there is a single
   * backing relational database. If enabled, the global jdbc connection settings
   * <strong>must</strong> be provided as {@code asio.jdbc} properties. Container specific settings
   * will be overridden.
   */
  public boolean globalDatasource = false;

  /**
   * Enable injection of schema specific jdbc credentials into deployed containers. This requires
   * the {@link #GLOBAL_DATASOURCE global datasource feature} to be enabled and mysql as backing
   * database. The user set on the global datasource must have sufficient permissions to create
   * new users with read and write permissions. Generally that means it must be a root account.
   * The global connections will <strong>never</strong> be used to execute client request.
   */
  public boolean multiTenancy = false;

  @Override
  public String toString() {
    return "AsioFeatures{" +
        "vphMetadata=" + vphMetadata +
        ", vphUriAuth=" + vphUriAuth +
        ", allowFederation=" + allowFederation +
        ", globalDatasource=" + globalDatasource +
        ", multiTenancy=" + multiTenancy +
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

  public boolean isGlobalDatasource() {
    return globalDatasource;
  }

  public void setGlobalDatasource(final boolean globalDatasource) {
    this.globalDatasource = globalDatasource;
  }

  public boolean isMultiTenancy() {
    return multiTenancy;
  }

  public void setMultiTenancy(final boolean multiTenancy) {
    this.multiTenancy = multiTenancy;
  }
}
