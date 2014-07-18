package at.ac.univie.isc.asio.jena;

import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Role;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.hp.hpl.jena.query.QueryExecution;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Handle general Jena query execution and serialization, independent from the actual query type
 * ({@code SELECT}, {@code ASK}, {@code DESCRIBE} or {@code CONSTRUCT}). The query must be
 * {@link JenaQueryHandler#execute()}  invoked} exactly once, then the results
 * are ready to be
 * {@link at.ac.univie.isc.asio.jena.JenaQueryHandler#write(java.io.OutputStream)}  serialized}.
 * Serialization <strong>MAY</strong> consume the result data.
 * <p>Any error during execution or serialization is rethrown.</p>
 */
public interface JenaQueryHandler extends Invocation {
  /**
   * This must be called before using a Handler.
   * @param query actual query instance to be executed
   * @return initialized handler
   */
  JenaQueryHandler init(QueryExecution query);

  /**
   * @return command to be executed
   */
  QueryExecution query();

  /**
   * Manage execution state and enforce invariants.
   * @param <RESULT> type of query result, i.e. ResultSet or Model
   */
  @NotThreadSafe
  @Nonnull
  abstract class BaseQueryHandler<RESULT> implements JenaQueryHandler {
    private final MediaType format;
    private QueryExecution query;
    private RESULT result;

    protected BaseQueryHandler(final MediaType format) {
      this.format = requireNonNull(format);
    }

    @Override
    public JenaQueryHandler init(final QueryExecution query) {
      this.query = requireNonNull(query);
      return this;
    }

    @Override
    public QueryExecution query() {
      return query;
    }

    @Override
    public Role requires() {
      return Role.READ;
    }

    @Override
    public final MediaType produces() {
      return format;
    }

    @Override
    public final void execute() {
      assert query != null : "not initialized";
      Preconditions.checkState(result == null, "query invoked twice");
      this.result = doInvoke(query);
      assert result != null : "implementation produced null result";
    }

    protected abstract RESULT doInvoke(QueryExecution execution);

    @Override
    public final void write(final OutputStream sink) {
      requireNonNull(sink);
      Preconditions.checkState(result != null, "no query results captured");
      doSerialize(sink, result);
    }

    protected abstract void doSerialize(OutputStream sink, RESULT data);

    @Override
    public final void cancel() {
      query.abort();
    }

    @Override
    public final void close() {
      query.close();
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("format", format)
          .add("query", query)
          .toString();
    }
  }
}
