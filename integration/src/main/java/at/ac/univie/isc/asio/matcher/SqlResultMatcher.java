/*
 * #%L
 * asio integration
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
