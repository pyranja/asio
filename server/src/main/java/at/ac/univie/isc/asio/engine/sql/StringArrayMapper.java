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
import org.jooq.Record;
import org.jooq.RecordMapper;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@VisibleForTesting
final class StringArrayMapper implements RecordMapper<Record, String[]> {
  private final ValuePresenter presenter;
  private String[] row;
  private Class<?>[] types;

  StringArrayMapper(final ValuePresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public String[] map(final Record record) {
    makeRow(record);
    cacheTypes(record);
    for (int idx = 0; idx < record.size(); idx++) {
      final Class<?> type = types[idx];
      final Object raw = record.getValue(idx, type);
      row[idx] = presenter.format(raw, type);
    }
    return row;
  }

  private void cacheTypes(final Record record) {
    if (types == null) {
      types = new Class<?>[record.size()];
      for (int idx = 0; idx < types.length; idx++) {
        types[idx] = record.field(idx).getDataType().getSQLDataType().getType();
        assert types[idx] != null : "null type cached";
      }
    }
    assert types.length == record.size() : "row size changed";
  }

  private void makeRow(final Record record) {
    if (row == null) {
      row = new String[record.size()];
    }
    assert row.length == record.size() : "row size changed";
  }
}
