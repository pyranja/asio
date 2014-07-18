package at.ac.univie.isc.asio.jooq;

import org.jooq.Cursor;
import org.jooq.Record;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

import at.ac.univie.isc.asio.DatasetException;
import at.ac.univie.isc.asio.engine.Invocation;
import at.ac.univie.isc.asio.security.Role;

final class SelectInvocation implements Invocation {
  interface CursorWriter {
    void serialize(OutputStream output, String statement, Cursor<Record> cursor) throws IOException;
  }

  private final CursorWriter writer;
  private final MediaType contentType;
  private final JdbcContext jdbc;
  private final String sql;

  private Cursor<Record> cursor;

  protected SelectInvocation(final JdbcContext jdbc, final String sql, final CursorWriter writer,
                             final MediaType contentType) {
    this.writer = writer;
    this.jdbc = jdbc;
    this.sql = sql;
    this.contentType = contentType;
  }

  @Override
  public Role requires() {
    return Role.READ;
  }

  @Override
  public final void execute() throws DatasetException {
    cursor = jdbc.query(sql);
  }

  @Override
  public MediaType produces() {
    return contentType;
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
    try (final Invocation me = this;) {
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
