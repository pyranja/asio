package at.ac.univie.isc.asio.jena;

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
 * {@link at.ac.univie.isc.asio.jena.JenaQueryHandler#invoke invoked} exactly once, then the results
 * are ready to be
 * {@link at.ac.univie.isc.asio.jena.JenaQueryHandler#serialize(java.io.OutputStream) serialized}.
 * Serialization <strong>MAY</strong> consume the result data.
 * <p>Any error during execution or serialization is rethrown.</p>
 */
public interface JenaQueryHandler {
  /**
   * Invoke the given query synchronously.
   * @param execution query descriptor
   */
  void invoke(QueryExecution execution);

  /**
   * Write the results of the executed query to the given byte stream.
   * @param sink byte stream where results will be written
   */
  void serialize(OutputStream sink);

  /**
   * @return MIME type of the serialized data
   */
  MediaType format();

  /**
   * Manage execution state and enforce invariants.
   * @param <RESULT> type of query result, i.e. ResultSet or Model
   */
  @NotThreadSafe
  @Nonnull
  abstract class BaseQueryHandler<RESULT> implements JenaQueryHandler {
    private final MediaType format;
    private RESULT result;

    protected BaseQueryHandler(final MediaType format) {
      this.format = requireNonNull(format);
    }

    @Override
    public final MediaType format() {
      return format;
    }

    @Override
    public final void invoke(final QueryExecution execution) {
      requireNonNull(execution);
      Preconditions.checkState(result == null, "query invoked twice");
      this.result = doInvoke(execution);
      assert result != null : "implementation produced null result";
    }

    protected abstract RESULT doInvoke(QueryExecution execution);

    @Override
    public final void serialize(final OutputStream sink) {
      requireNonNull(sink);
      Preconditions.checkState(result != null, "no query results captured");
      doSerialize(sink, result);
    }

    protected abstract void doSerialize(OutputStream sink, RESULT data);

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("format", format)
          .toString();
    }
  }
}
