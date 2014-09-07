package at.ac.univie.isc.asio.sql;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.io.Resources;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.util.fileloader.DataFileLoader;
import org.dbunit.util.fileloader.FlatXmlDataFileLoader;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.lang.annotation.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

public class EmbeddedDb extends ExternalResource {
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface Dataset {
    String value();
  }

  public static EmbeddedDbBuilderStart memory() {
    return new EmbeddedDbBuilder(INMEM_URL);
  }

  public static final String INMEM_URL = "jdbc:h2:mem:test";
  public static final String DEF_USER = "root";
  public static final String DEF_PASSWORD = "change";

  private final String jdbcUrl;
  private final URL schema;
  // state
  private JdbcConnectionPool pool;

  EmbeddedDb(final String jdbcUrl, final String schemaPath) {
    this.jdbcUrl = requireNonNull(jdbcUrl, "JDBC URL");
    requireNonNull(schemaPath, "path to schema file");
    this.schema = this.getClass().getResource(schemaPath);
    requireNonNull(schema, "schema "+ schemaPath +" file not found");
  }

  public DataSource dataSource() {
    assert pool != null : "EmbeddedDb not initialized";
    return pool;
  }

  public Connection connection() throws SQLException {
    return dataSource().getConnection();
  }

  public Table<Integer, String, String> reference(final String query) throws SQLException {
    try (
        final Connection conn = connection();
        final PreparedStatement stmt = conn.prepareStatement(query);
        final ResultSet rs = stmt.executeQuery()
    ) {
      return ConvertToTable.fromResultSet(rs);
    }
  }

  public PopulateDb defaultDataset(final String path) {
    return new PopulateDb(dataSource(), path);
  }

  public PopulateDb populate() {
    return new PopulateDb(dataSource(), null);
  }

  @Override
  protected void before() throws Throwable {
    pool = JdbcConnectionPool.create(jdbcUrl, DEF_USER, DEF_PASSWORD);
    final String ddl = Resources.toString(schema, Charsets.UTF_8);
    createSchema(ddl);
  }

  private void createSchema(final String ddl) throws SQLException {
    try (final Connection conn = connection();
      final java.sql.Statement stmt = conn.createStatement()) {
      stmt.execute(ddl);
    }
  }

  @Override
  protected void after() {
    pool.dispose();
    pool = null;
  }

  public static interface EmbeddedDbBuilderStart {
    EmbeddedDbBuilderFinish schema(final String schemaPath);
  }

  public static interface EmbeddedDbBuilderFinish {
    EmbeddedDb create();
  }

  public static class EmbeddedDbBuilder implements EmbeddedDbBuilderStart, EmbeddedDbBuilderFinish {

    private final String jdbcUrl;
    private String schemaPath = null;

    EmbeddedDbBuilder(final String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    public EmbeddedDbBuilder schema(final String schemaPath) {
      this.schemaPath = schemaPath;
      return this;
    }

    public EmbeddedDb create() {
      return new EmbeddedDb(jdbcUrl, schemaPath);
    }
  }

  public static class PopulateDb implements TestRule {
    public static final DataFileLoader LOADER = new FlatXmlDataFileLoader();

    private final IDatabaseTester dbUnit;
    private final String defaultPath;

    public PopulateDb(final DataSource dataSource, @Nullable final String defaultPath) {
      dbUnit = new DataSourceDatabaseTester(dataSource);
      this.defaultPath = defaultPath;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
      final Optional<String> datasetPath = findDatasetPath(description);
      if (datasetPath.isPresent()) {
        return new Statement() {
          @Override
          public void evaluate() throws Throwable {
            final IDataSet dataset = LOADER.load(datasetPath.get());
            dbUnit.setDataSet(dataset);
            dbUnit.onSetup();
            try {
              base.evaluate();
            } finally {
              dbUnit.onTearDown();
            }
          }
        };
      } else {  // skip dataset loading if no annotation present
        return base;
      }
    }

    private Optional<String> findDatasetPath(final Description description) {
      final String path;
      Dataset annotation = description.getAnnotation(Dataset.class);
      if (annotation == null) {
        path = defaultPath;
      } else {
        path = annotation.value();
      }
      return Optional.fromNullable(path);
    }
  }
}
