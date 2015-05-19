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

import at.ac.univie.isc.asio.ObjectFactory;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Table;
import org.jooq.Catalog;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Schema;

import static java.util.Objects.requireNonNull;

/**
 * Encapsulate transformation from Jooq model classes to Asio JAXB representations.
 */
public final class SqlSchemaBuilder {
  private static final ObjectFactory JAXB = new ObjectFactory();

  public static SqlSchemaBuilder create() {
    return new SqlSchemaBuilder();
  }

  private final SqlSchema product;
  private String activeCatalog;
  private String activeSchema;

  private SqlSchemaBuilder() {
    product = JAXB.createSqlSchema();
  }

  public SqlSchemaBuilder switchCatalog(final Catalog catalog) {
    this.activeCatalog = requireNonNull(catalog.getName());
    return this;
  }

  public SqlSchemaBuilder noCatalog() {
    this.activeCatalog = null;
    return this;
  }

  public SqlSchemaBuilder switchSchema(final Schema schema) {
    this.activeSchema = requireNonNull(schema.getName());
    return this;
  }

  public SqlSchemaBuilder noSchema() {
    this.activeSchema = null;
    return this;
  }

  public SqlSchemaBuilder add(final org.jooq.Table<?> sqlTable) {
    final Table metaTable = JAXB.createTable()
        .withCatalog(activeCatalog)
        .withSchema(activeSchema)
        .withName(sqlTable.getName())
        ;
    for (Field<?> field : sqlTable.fields()) {
      final DataType<?> sqlType = field.getDataType();
      final XmlSchemaType xmlType = XmlSchemaType.fromJavaType(sqlType.getType());
      metaTable.getColumn().add(JAXB.createColumn()
          .withName(field.getName())
          .withType(xmlType.qname())
          .withSqlType(sqlType.getTypeName())
          .withLength(sqlType.length()));
    }
    product.getTable().add(metaTable);
    return this;
  }

  public SqlSchema build() {
    return product;
  }
}
