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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import org.jooq.Cursor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
final class CsvWriter implements SelectInvocation.CursorWriter {
  @VisibleForTesting
  static RecordMapper<Record, String[]> RECORD_MAPPER() {
    final ValuePresenter presenter = ValuePresenter
        .withDefault(ValuePresenter.FAIL)
        .register(Representations.plainNull(), Void.class)
        .register(Representations.xsBoolean(), Boolean.class)
        .register(Representations.quotedString(), String.class)
        .register(Representations.xsDateTime(), Timestamp.class)
        .register(Representations.xsDate(), Date.class)
        .register(Representations.xsTime(), Time.class)
        .register(Representations.xsLong(), Long.class, Integer.class, Short.class, Byte.class)
        .register(Representations.xsDecimal(), Number.class, BigDecimal.class, BigInteger.class)
        .register(Representations.xsDouble(), Double.class, Float.class)
        .register(Representations.plainBinary(), byte[].class)
        .build();
    return new StringArrayMapper(presenter);
  }

  private static final Joiner CSV_JOINER = Joiner.on(",");

  private OutputStreamWriter sink;

  @Override
  public void serialize(final OutputStream sink, final String ignored, final Cursor<Record> cursor) throws IOException {
    this.sink = new OutputStreamWriter(sink, Charsets.UTF_8);
    header(cursor);
    rows(cursor);
    this.sink.flush();
  }

  private void header(final Cursor<Record> cursor) throws IOException {
    final Field<?>[] columns = cursor.fields();
    final String[] names = new String[columns.length];
    for (int idx = 0; idx < columns.length; idx++) {
      final Field<?> column = columns[idx];
      names[idx] = column.getName();
    }
    writeRow(names);
  }

  private void rows(final Cursor<Record> cursor) throws IOException {
    final RecordMapper<Record, String[]> mapper = RECORD_MAPPER();
    while (cursor.hasNext()) {
      final String[] values = cursor.fetchOne(mapper);
      writeRow(values);
    }
  }

  private Writer writeRow(final String[] values) throws IOException {
    return CSV_JOINER.appendTo(this.sink, values).append('\r').append('\n');
  }

}
