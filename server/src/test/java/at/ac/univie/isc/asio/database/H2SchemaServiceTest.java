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
package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.Column;
import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.ObjectFactory;
import at.ac.univie.isc.asio.SqlSchema;
import at.ac.univie.isc.asio.Table;
import at.ac.univie.isc.asio.engine.sql.XmlSchemaType;
import at.ac.univie.isc.asio.io.Classpath;
import at.ac.univie.isc.asio.sql.Database;
import com.google.common.collect.Iterables;
import org.jooq.impl.SQLDataType;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class H2SchemaServiceTest {
  private final Database db = Database
      .create("jdbc:h2:mem:public;DB_CLOSE_DELAY=-1;MODE=MYSQL").build()
      .execute(Classpath.read("database/H2SchemaServiceTest-schema.sql"));

  private final H2SchemaService subject = new H2SchemaService(db.datasource());

  @Test
  public void default_schema_is_called__public__() throws Exception {
    final Database db = Database.create("jdbc:h2:mem:").build();
    final com.google.common.collect.Table<Integer, String, String> result =
        db.reference("SELECT SCHEMA()");
    assertThat(result.values(), hasItem("public"));
  }

  @Test
  public void fetches_public_table_metadata() throws Exception {
    final SqlSchema schema = subject.explore(Id.valueOf("test"));
    final List<Table> tables = schema.getTable();
    assertThat(tables, hasSize(1));
  }

  @Test
  public void validate_extracted_metadata() throws Exception {
    final SqlSchema schema = subject.explore(Id.valueOf("test"));
    final Table table = Iterables.getOnlyElement(schema.getTable());
    assertThat(table.getName(), equalTo("sample"));
    assertThat(table.getSchema(), equalTo("test"));
    assertThat(table.getCatalog(), equalTo("public"));
  }

  @Test
  public void validate_extracted_columns() throws Exception {
    final ObjectFactory jaxb = new ObjectFactory();
    final SqlSchema schema = subject.explore(Id.valueOf("test"));
    final Table table = Iterables.getOnlyElement(schema.getTable());
    final Column idColumn = jaxb.createColumn()
        .withName("id")
        .withType(XmlSchemaType.LONG.qname())
        .withSqlType(SQLDataType.INTEGER.getTypeName())
        .withLength(10);
    final Column dataColumn = jaxb.createColumn()
        .withName("data")
        .withType(XmlSchemaType.STRING.qname())
        .withSqlType(SQLDataType.VARCHAR.getTypeName())
        .withLength(255);
    assertThat(table.getColumn(), containsInAnyOrder(idColumn, dataColumn));
  }
}
