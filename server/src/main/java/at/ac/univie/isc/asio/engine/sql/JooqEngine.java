package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.AsioError;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import com.google.common.base.Supplier;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

/**
 * SQL query engine using JOOQ.
 */
public final class JooqEngine implements Engine {

  /**
   * Create sql engine from a connection pool and specification.
   * @param pool connection pool
   * @param spec jdbc settings
   * @return initialized engine
   */
  public static <POOL extends DataSource & AutoCloseable> JooqEngine create(final POOL pool, final JdbcSpec spec) {
    return new JooqEngine(new JdbcFactory<>(pool, spec));
  }

  /**
   * Indicate, that his invocation has been cancelled before completion.
   */
  static final class Cancelled extends AsioError.Base {
    public Cancelled() {
      super("query cancelled");
    }
  }

  public static final String PARAM_QUERY = "query";
  public static final String PARAM_UPDATE = "update";

  public static final MediaType CSV_TYPE = MediaType.valueOf("text/csv");
  public static final MediaType WEBROWSET_TYPE = MediaType.valueOf("application/webrowset+xml");
  public static final MediaType XML_RESULTS = MediaType.valueOf("application/sql-results+xml");

  private final TypeMatchingResolver<SelectInvocation.CursorWriter> queryRegistry;
  private final TypeMatchingResolver<UpdateInvocation.ModCountWriter> updateRegistry;

  private final JdbcFactory<?> state;

  private JooqEngine(final JdbcFactory<?> state) {
    this.state = state;
    queryRegistry = TypeMatchingResolver.<SelectInvocation.CursorWriter>builder()
        .register(WEBROWSET_TYPE, new Supplier<SelectInvocation.CursorWriter>() {
          @Override
          public SelectInvocation.CursorWriter get() {
            return new WebRowSetWriter();
          }
        })
        .alias(XML_RESULTS)
        .alias(MediaType.APPLICATION_XML_TYPE)
        .register(CSV_TYPE, new Supplier<SelectInvocation.CursorWriter>() {
          @Override
          public SelectInvocation.CursorWriter get() {
            return new CsvWriter();
          }
        })
        .make();
    updateRegistry = TypeMatchingResolver.<UpdateInvocation.ModCountWriter>builder()
        .register(XML_RESULTS, new Supplier<UpdateInvocation.ModCountWriter>() {
          @Override
          public UpdateInvocation.ModCountWriter get() {
            return new UpdateInvocation.XmlModCountWriter();
          }
        })
        .alias(MediaType.APPLICATION_XML_TYPE)
        .register(CSV_TYPE, new Supplier<UpdateInvocation.ModCountWriter>() {
          @Override
          public UpdateInvocation.ModCountWriter get() {
            return new UpdateInvocation.CsvModCountWriter();
          }
        })
        .make();
  }

  @Override
  public Language language() {
    return Language.SQL;
  }

  @Override
  public void close() {
    this.state.close();
  }

  @Override
  public Invocation prepare(final Command command) {
    final JdbcExecution execution = new JdbcExecution(state);
    final Invocation invocation;
    if (command.properties().containsKey(PARAM_QUERY)) {
      invocation = createSelect(command, execution);
    } else if (command.properties().containsKey(PARAM_UPDATE)) {
      invocation = createUpdate(command, execution);
    } else {
      throw new Command.IllegalCommand("parameter " + PARAM_QUERY + " or " + PARAM_UPDATE + " is required");
    }
    return invocation;
  }

  private Invocation createUpdate(final Command command, final JdbcExecution execution) {
    final String sql = command.require(PARAM_UPDATE);
    final TypeMatchingResolver.Selection<UpdateInvocation.ModCountWriter> selection
        = updateRegistry.select(command.acceptable());
    return new UpdateInvocation(execution, sql, selection.value(), selection.type());
  }

  private Invocation createSelect(final Command command, final JdbcExecution execution) {
    final String sql = command.require(PARAM_QUERY);
    final TypeMatchingResolver.Selection<SelectInvocation.CursorWriter> selection
        = queryRegistry.select(command.acceptable());
    return new SelectInvocation(execution, sql, selection.value(), selection.type());
  }
}
