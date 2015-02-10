package at.ac.univie.isc.asio.matcher;

import at.ac.univie.isc.asio.sql.ConvertToTable;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Table;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import javax.sql.rowset.RowSetProvider;
import javax.sql.rowset.WebRowSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

abstract class SqlResultMatcher extends TypeSafeMatcher<String> {
  static class Csv extends SqlResultMatcher {
    Csv(final Table<Integer, String, String> expected) {
      super(expected);
    }

    @Override
    protected Table<Integer, String, String> parse(final InputStream data) throws IOException {
      return ConvertToTable.fromCsv(data);
    }
  }

  static class Webrowset extends SqlResultMatcher {
    Webrowset(final Table<Integer, String, String> expected) {
      super(expected);
    }

    @Override
    protected Table<Integer, String, String> parse(final InputStream data) throws IOException, SQLException {
      final WebRowSet webRowSet = RowSetProvider.newFactory().createWebRowSet();
      webRowSet.readXml(data);
      return ConvertToTable.fromResultSet(webRowSet);
    }
  }

  private final Table<Integer, String, String> expected;

  SqlResultMatcher(final Table<Integer, String, String> expected) {
    this.expected = expected;
  }

  protected abstract Table<Integer, String, String> parse(final InputStream data) throws Exception;

  @Override
  protected boolean matchesSafely(final String item) {
    return expected.equals(doConvert(item));
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(" sql result-set containing ").appendValue(expected);
  }

  @Override
  protected void describeMismatchSafely(final String item, final Description mismatchDescription) {
    mismatchDescription.appendText("was ").appendValue(doConvert(item));
  }

  private Table<Integer, String, String> doConvert(final String item) {
    try {
      final ByteArrayInputStream raw = new ByteArrayInputStream(item.getBytes(Charsets.UTF_8));
      return parse(raw);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
