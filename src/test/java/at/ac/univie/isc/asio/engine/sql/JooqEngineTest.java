package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.DatasetUsageException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.SqlResult;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.engine.Parameters;
import at.ac.univie.isc.asio.security.Role;
import at.ac.univie.isc.asio.security.Token;
import at.ac.univie.isc.asio.sql.EmbeddedDb;
import at.ac.univie.isc.asio.sql.ConvertToTable;
import com.google.common.collect.Table;
import com.google.common.io.ByteStreams;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.conf.SettingsTools;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.sql.rowset.RowSetProvider;
import javax.sql.rowset.WebRowSet;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;

public class JooqEngineTest {
  public static final MediaType SQL_RESULTS_TYPE = MediaType.valueOf("application/sql-results+xml");
  @ClassRule
  public static EmbeddedDb DB =
      EmbeddedDb.memory().schema("/database/JooqEngineTest-schema.sql").create();
  @Rule
  public EmbeddedDb.PopulateDb populate = DB.defaultDataset("/database/JooqEngineTest-data.xml");
  @Rule
  public ExpectedException error = ExpectedException.none();

  private DSLContext jooqContext;
  private JooqEngine subject;

  @Before
  public void setUp() {
    final Settings options = SettingsTools.defaultSettings().withExecuteLogging(true);
    jooqContext = DSL.using(DB.dataSource(), SQLDialect.H2, options);
    subject = new JooqEngine(jooqContext, TimeoutSpec.undefined());
  }

  // ========= VALID QUERIES
  public static final String REFERENCE_EMPTY =
      "SELECT ID,EXPECT,COL_BOOLEAN,COL_STRING,COL_LONG,COL_DECIMAL,COL_DOUBLE FROM test WHERE 1=0";
  public static final String REFERENCE_SELECT =
      "SELECT ID,EXPECT,COL_BOOLEAN,COL_STRING,COL_LONG,COL_DECIMAL,COL_DOUBLE FROM test ORDER BY id";

  // CSV format : RFC4180
  public static final MediaType CSV_TYPE = MediaType.valueOf("text/csv");

  public static final String HEADERS_ONLY =
      "ID,EXPECT,COL_BOOLEAN,COL_STRING,COL_LONG,COL_DECIMAL,COL_DOUBLE\r\n";

  @Test
  public void empty_sql_select_to_csv_header() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY,
            REFERENCE_EMPTY)
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = performInvocationWith(params);
    assertThat(new String(raw), is(equalToIgnoringWhiteSpace(HEADERS_ONLY)));
  }

  @Test
  public void valid_sql_select_to_csv_header() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT)
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = performInvocationWith(params);
    final Table<Integer, String, String> result = ConvertToTable.fromCsv(new ByteArrayInputStream(raw));
    assertThat(result.columnKeySet(),
        containsInAnyOrder("ID", "EXPECT", "COL_BOOLEAN", "COL_STRING", "COL_LONG", "COL_DECIMAL", "COL_DOUBLE"));
  }

  public static final String EXPECTED_CSV =
      "ID,EXPECT,COL_BOOLEAN,COL_STRING,COL_LONG,COL_DECIMAL,COL_DOUBLE\r\n"
          + "0,\"default\",true,\"default\",0,0.0,0.0E0\r\n"
          + "1,\"null\",null,null,null,null,null\r\n"
          + "2,\"negative\",false,\"negative\",-1,-1.0,-1.0E0\r\n"
          + "3,\"positive\",true,\"positive\",1,1.0,1.0E0\r\n"
          + "4,\"common\",true,\"common\",123456789,123.456789,1.23456789E2\r\n";

  @Test
  public void valid_sql_select_to_csv_values() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT)
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = performInvocationWith(params);
    assertThat(new String(raw), is(equalToIgnoringWhiteSpace(EXPECTED_CSV)));
  }

  // WEBROWSET format (ensure reads back into WebRowSet!)
  public static final MediaType WEBROWSET_TYPE = MediaType.valueOf("application/webrowset+xml");

  public static final String[] COLUMN_NAMES = new String[]
      { "ID", "EXPECT", "COL_BOOLEAN", "COL_STRING", "COL_LONG", "COL_DECIMAL", "COL_DOUBLE" };

  @Test
  public void empty_select_to_webrowset_header() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_EMPTY)
        .accept(WEBROWSET_TYPE)
        .build();
    final byte[] raw = performInvocationWith(params);
    final WebRowSet wrs = parseWebRowSet(raw);
    assertThat(wrs.size(), is(0));
    final ResultSetMetaData context = wrs.getMetaData();
    for (int index = 0; index < COLUMN_NAMES.length; index++) {
      assertThat(context.getColumnName(index+1), is(COLUMN_NAMES[index]));
    }
  }

  @Test
  public void valid_select_to_webrowset_content() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT)
        .accept(WEBROWSET_TYPE)
        .build();
    final byte[] raw = performInvocationWith(params);
    final WebRowSet wrs = parseWebRowSet(raw);
    assertThat(wrs.size(), is(5));
    final Table<Integer, String, String> expected = DB.reference(REFERENCE_SELECT);
    final Table<Integer, String, String> actual = ConvertToTable.fromResultSet(wrs);
    assertThat(actual, is(expected));
  }

  private WebRowSet parseWebRowSet(final byte[] raw) throws SQLException, IOException {
    final WebRowSet wrs = RowSetProvider.newFactory().createWebRowSet();
    wrs.readXml(new ByteArrayInputStream(raw));
    return wrs;
  }

  // XML && JSON formats

  // ========= VALID UPDATES
  public static final String REFERENCE_UPDATE = "INSERT INTO updates(id, data) VALUES (0, 'test');";

  @Test
  public void update_to_xml() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_UPDATE, REFERENCE_UPDATE)
        .accept(SQL_RESULTS_TYPE)
        .build();
    final byte[] bytes = performInvocationWith(params);
    final SqlResult results = JAXB.unmarshal(new ByteArrayInputStream(bytes), SqlResult.class);
    assertThat(results.getHead().getStatement(), is(REFERENCE_UPDATE));
    assertThat(results.getUpdate().getAffected(), is(1L));
  }

  @Test
  public void update_to_csv() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_UPDATE, REFERENCE_UPDATE)
        .accept(CSV_TYPE)
        .build();
    final byte[] raw = performInvocationWith(params);
    final Table<Integer, String, String> result =
        ConvertToTable.fromCsv(new ByteArrayInputStream(raw));
    assertThat(result.size(), is(2));
    assertThat(result.get(0, "statement"), is(equalToIgnoringCase(REFERENCE_UPDATE)));
    assertThat(result.get(0, "affected"), is("1"));
  }

  private byte[] performInvocationWith(final Parameters params) throws IOException {
    try (final Invocation invocation = subject.prepare(params, Token.ANONYMOUS)) {
      invocation.execute();
      final ByteArrayOutputStream sink = new ByteArrayOutputStream();
      invocation.write(sink);
      return sink.toByteArray();
    }
  }

  // ========= BEHAVIOR

  @Test
  public void query_invocation_with_dml_fails() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_UPDATE)
        .accept(MediaType.WILDCARD_TYPE)
        .build();
    error.expect(DatasetException.class);
    performInvocationWith(params);
  }

  @Test
  public void update_invocation_with_select_fails() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_UPDATE, REFERENCE_SELECT)
        .accept(SQL_RESULTS_TYPE).build();
    error.expect(DatasetException.class);
    performInvocationWith(params);
  }

  @Test
  public void query_invocation_must_not_alter_db() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, "INSERT INTO updates(id, data) VALUES (0, 'test');")
        .accept(MediaType.WILDCARD_TYPE).build();
    try {
      performInvocationWith(params);
    } catch (final Exception ignored) {
    }
    assertThat(DB.reference("SELECT * FROM updates").values(), is(empty()));
  }

  @Test
  public void cancel_interrupts_query() throws Exception {
    final Parameters params = Parameters
        .builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT)
        .accept(CSV_TYPE)
        .build();
    final Invocation invocation = subject.prepare(params, Token.ANONYMOUS);
    error.expect(JooqEngine.Cancelled.class);
    invocation.execute();
    invocation.cancel();
    invocation.write(ByteStreams.nullOutputStream());
  }

  @Test
  public void query_invocation_requires_read_role() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT)
        .accept(CSV_TYPE)
        .build();
    final Invocation invocation = subject.prepare(params, Token.ANONYMOUS);
    assertThat(invocation.requires(), is(Role.READ));
  }

  @Test
  public void update_invocation_requires_write_role() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .single(JooqEngine.PARAM_UPDATE, REFERENCE_UPDATE)
        .accept(CSV_TYPE)
        .build();
    final Invocation invocation = subject.prepare(params, Token.ANONYMOUS);
    assertThat(invocation.requires(), is(Role.WRITE));
  }

  // ========= ILLEGAL INPUT

  @Test
  public void missing_query() throws Exception {
    final Parameters params = Parameters.builder(Language.SQL)
        .accept(MediaType.WILDCARD_TYPE).build();
    error.expect(DatasetUsageException.class);
    subject.prepare(params, Token.ANONYMOUS);
  }

  @Test
  public void no_format_accepted() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT).build();
    error.expect(DatasetUsageException.class);
    subject.prepare(params, Token.ANONYMOUS);
  }

  @Test
  public void no_supported_format() throws Exception {
    final Parameters params = Parameters.builder(Language.SPARQL)
        .single(JooqEngine.PARAM_QUERY, REFERENCE_SELECT)
        .accept(MediaType.valueOf("image/jpeg")).build();
    error.expect(DatasetUsageException.class);
    subject.prepare(params, Token.ANONYMOUS);
  }
}
