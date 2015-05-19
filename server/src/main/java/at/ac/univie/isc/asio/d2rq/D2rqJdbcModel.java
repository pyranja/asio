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
package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.Beans;
import at.ac.univie.isc.asio.tool.JdbcTools;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

import java.util.Map;

/**
 * Extract jdbc database from a d2rq mapping. Expect a single jdbc configuration block.
 */
public final class D2rqJdbcModel {
  /**
   * Create new mapping visitor, that finds jdbc database.
   *
   * @return initialized visitor
   */
  static D2rqJdbcModel parse(final Mapping mapping) {
    final D2rqJdbcModel finder = new D2rqJdbcModel();
    for (Database database : mapping.databases()) {
      finder.visit(database);
    }
    if (!finder.visited) { throw new InvalidD2rqConfig(D2RQ.Database, "missing"); }
    return finder;
  }

  private boolean visited = false;

  private String schema;
  private String url;
  private String driver;
  private String username;
  private String password;
  private Map<String, String> properties;

  D2rqJdbcModel() { /* --- */ }

  void visit(final Database database) {
    if (visited) { throw new InvalidD2rqConfig(D2RQ.Database, "found more than one"); }
    visited = true;
    url = database.getJDBCDSN();
    driver = database.getJDBCDriver();
    username = nullToEmpty(database.getUsername());
    password = nullToEmpty(database.getPassword());
    schema = database.getConnectionProperties().getProperty("schema");
    properties = Beans.copyToMap(database.getConnectionProperties(), "schema");
    if (!JdbcTools.isValidJdbcUrl(url)) {
      throw new InvalidD2rqConfig(D2RQ.jdbcDSN, "<" + url + "> is not valid");
    }
    visited = true;
  }

  public String getSchema() {
    return schema;
  }

  public String getUrl() {
    return url;
  }

  public String getDriver() {
    return driver;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  private String nullToEmpty(final String raw) {
    return raw == null ? "" : raw;
  }
}
