package at.ac.univie.isc.asio.engine.sql;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Permission;
import org.jooq.Cursor;
import org.jooq.Record;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;

final class SelectInvocation extends SqlInvocation {
  interface CursorWriter {
    void serialize(OutputStream output, String statement, Cursor<Record> cursor) throws IOException;
  }

  private final CursorWriter writer;

  private Cursor<Record> cursor;

  public SelectInvocation(final JdbcExecution jdbc, final String sql, final CursorWriter writer,
                          final MediaType contentType) {
    super(jdbc, sql, contentType, Permission.READ);
    this.writer = writer;
  }

  @Override
  public final void execute() throws DatasetException {
    cursor = jdbc.query(sql);
  }

  @Override
  public final void write(final OutputStream output) throws IOException {
    assert cursor != null : "not executed";
    if (cursor.isClosed()) {
      throw new JooqEngine.Cancelled();
    }
    try {
      writer.serialize(output, sql, cursor);
    } finally {
      close();
    }
  }

  @Override
  public final void cancel() {
    try (final Invocation me = this) {
      jdbc.cancel();
    }
  }

  @Override
  public final void close() {
    try {
      if (cursor != null) {
        cursor.close();
      }
    } finally {
      jdbc.close();
    }
  }
}
