package at.ac.univie.isc.asio.engine.sparql;

import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Permission;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.util.Context;
import org.openjena.atlas.io.IndentedLineBuffer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.ws.rs.core.MediaType;
import java.io.OutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Manage execution state and enforce invariants.
 * @param <RESULT> type of query result, i.e. ResultSet or Model
 */
@NotThreadSafe
@Nonnull
public abstract class SparqlInvocation<RESULT> implements Invocation {
  private final MediaType format;
  private Multimap<String, String> properties;
  private QueryExecution query;
  private RESULT result;

  public SparqlInvocation(final MediaType format) {
    this.format = requireNonNull(format);
  }

  public SparqlInvocation<RESULT> init(final QueryExecution query) {
    this.query = requireNonNull(query);
    final Context context = query.getContext();
    properties = ImmutableMultimap.<String, String>builder()
        .put("command", format(query.getQuery()))
        .put("permission", Permission.INVOKE_QUERY.toString())
        .put("format", format.toString())
        .put("engine", "jena")
        .put("timeout", context.getAsString(ARQ.queryTimeout, "undefined"))
        .build();
    return this;
  }

  public QueryExecution query() {
    return query;
  }

  @Override
  public final Multimap<String, String> properties() {
    return properties;
  }

  @Override
  public final Permission requires() {
    return Permission.INVOKE_QUERY;
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
        .add("properties", properties)
        .toString();
  }

  static String format(final Query query) {
    final IndentedLineBuffer buffer = new IndentedLineBuffer();
    buffer.setFlatMode(true);
    buffer.setUnitIndent(0);
    query.serialize(buffer, Syntax.syntaxARQ);
    return buffer.asString();
  }
}
