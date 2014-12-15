package at.ac.univie.isc.asio.engine.sql;

import com.google.common.collect.Iterables;
import org.jooq.Catalog;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jooq.impl.DSL.field;
import static org.junit.Assert.assertThat;

/**
 * Explore MySQL schema through jooq.Meta
 */
@Ignore("explorative - depends on external setup")
public class MysqlSchemaExploration {
  private Connection connection;
  private DSLContext db;

  @Before
  public void connect() throws SQLException {
    connection = DriverManager.getConnection("jdbc:mysql://localhost:3350/test", "root", "change");
    db = DSL.using(connection);
  }

  @After
  public void cleanUp() throws SQLException {
    connection.close();
  }

  @Test
  public void has_a_single_unnamed_catalog() throws Exception {
    final List<Catalog> catalogs = db.meta().getCatalogs();
    assertThat(catalogs, hasSize(1));
    assertThat(catalogs.get(0).getName(), is(""));
  }

  @Test
  public void has_well_known_internal_schemas() throws Exception {
    final Catalog catalog = Iterables.getOnlyElement(db.meta().getCatalogs());
    assertThat(catalog.getSchema("mysql"), is(notNullValue()));
    assertThat(catalog.getSchema("information_schema"), is(notNullValue()));
    assertThat(catalog.getSchema("performance_schema"), is(notNullValue()));
  }

  @Test
  public void can_fetch_active_database() throws Exception {
    final Field<String> activeSchema = field("DATABASE()", String.class).as("schema");
    final String schema = db.select(activeSchema).fetchOne(activeSchema);
    assertThat(schema, is("test"));
  }
}
