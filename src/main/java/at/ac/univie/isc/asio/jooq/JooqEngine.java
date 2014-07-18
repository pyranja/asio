package at.ac.univie.isc.asio.jooq;

import at.ac.univie.isc.asio.DatasetFailureException;
import at.ac.univie.isc.asio.Language;
import at.ac.univie.isc.asio.config.TimeoutSpec;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.engine.TypeMatchingResolver;
import at.ac.univie.isc.asio.protocol.Parameters;
import com.google.common.base.Supplier;
import org.jooq.DSLContext;

import javax.ws.rs.core.MediaType;
import java.security.Principal;

import static java.util.Objects.requireNonNull;

public final class JooqEngine implements Engine {

  static final class Cancelled extends DatasetFailureException {

    public Cancelled() {
      super("query cancelled", null);
    }
  }

  public static final String PARAM_QUERY = "query";
  public static final String PARAM_UPDATE = "update";

  public static final MediaType CSV_TYPE = MediaType.valueOf("text/csv");
  public static final MediaType WEBROWSET_TYPE = MediaType.valueOf("application/webrowset+xml");
  public static final MediaType XML_RESULTS = MediaType.valueOf("application/sql-results+xml");

  private final TypeMatchingResolver<SelectInvocation.CursorWriter> queryRegistry;
  private final TypeMatchingResolver<UpdateInvocation.ModCountWriter> updateRegistry;

  private final DSLContext create;
  private final TimeoutSpec timeout;

  public JooqEngine(final DSLContext create, final TimeoutSpec timeout) {
    this.create = requireNonNull(create);
    this.timeout = requireNonNull(timeout);
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
  public Invocation create(final Parameters parameters, final Principal ignored) {
    final JdbcContext execution = new JdbcContext(create, timeout);
    final Invocation invocation;
    if (parameters.properties().containsKey(PARAM_QUERY)) {
      invocation = createSelect(parameters, execution);
    } else if (parameters.properties().containsKey(PARAM_UPDATE)) {
      invocation = createUpdate(parameters, execution);
    } else {
      throw new Parameters.MissingParameter(PARAM_QUERY + " or " + PARAM_UPDATE);
    }
    return invocation;
  }

  private Invocation createUpdate(final Parameters parameters, final JdbcContext execution) {
    final String sql = parameters.require(PARAM_UPDATE);
    final TypeMatchingResolver.Selection<UpdateInvocation.ModCountWriter> selection
        = updateRegistry.select(parameters.acceptable());
    return new UpdateInvocation(execution, sql, selection.value(), selection.type());
  }

  private Invocation createSelect(final Parameters parameters, final JdbcContext execution) {
    final String sql = parameters.require(PARAM_QUERY);
    final TypeMatchingResolver.Selection<SelectInvocation.CursorWriter> selection
        = queryRegistry.select(parameters.acceptable());
    return new SelectInvocation(execution, sql, selection.value(), selection.type());
  }
}
