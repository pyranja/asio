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
