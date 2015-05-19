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
package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Permission;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import javax.ws.rs.core.MediaType;

abstract class SqlInvocation implements Invocation {
  private final Multimap<String, String> properties;
  private final MediaType format;
  private final Permission requiredPermission;
  protected final JdbcExecution jdbc;
  protected final String sql;

  public SqlInvocation(final JdbcExecution jdbc, final String sql, final MediaType format, final Permission requiredPermission) {
    this.format = format;
    this.sql = sql;
    this.jdbc = jdbc;
    this.requiredPermission = requiredPermission;
    properties = captureProperties();
  }

  private Multimap<String, String> captureProperties() {
      return ImmutableListMultimap.<String, String>builder()
        .put("command", Pretty.compact(sql))
        .put("permission", requiredPermission.toString())
        .put("format", format.toString())
        .put("engine", "jooq")
        .build();
  }

  @Override
  public final Multimap<String, String> properties() {
    return properties;
  }

  @Override
  public final Permission requires() {
    return requiredPermission;
  }

  @Override
  public final MediaType produces() {
    return format;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("properties", properties)
        .toString();
  }
}
