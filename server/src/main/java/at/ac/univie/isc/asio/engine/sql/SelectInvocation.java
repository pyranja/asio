/*
 * #%L
 * asio server
 * %%
 * Copyright (C) 2013 - 2015 Research Group Scientific Computing, University of Vienna
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package at.ac.univie.isc.asio.engine.sql;

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
    super(jdbc, sql, contentType, Permission.INVOKE_QUERY);
    this.writer = writer;
  }

  @Override
  public final void execute() {
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
