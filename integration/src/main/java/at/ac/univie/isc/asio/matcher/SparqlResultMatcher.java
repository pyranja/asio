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

import at.ac.univie.isc.asio.io.Payload;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Attempt to parse a string as SPARQL ResultSet and compare it against an expected one.
 */
final class SparqlResultMatcher extends TypeSafeMatcher<String> {
  private final ResultSetRewindable expected;
  private final ResultsFormat format;

  private SparqlResultMatcher(final ResultSet expected, final ResultsFormat format) {
    // dump in expected format and parse again, to ensure expected result set takes into account
    // lossy formats (e.g. CSV loses literal type information).
    this.format = format;
    this.expected = read(asString(expected));
  }

  static SparqlResultMatcher create(final ResultSet expected, final ResultsFormat format) {
    return new SparqlResultMatcher(expected, format);
  }

  @Override
  protected boolean matchesSafely(final String item) {
    expected.reset();
    final ResultSet actual = read(item);
    return ResultSetCompare.equalsByValue(expected, actual);
  }

  @Override
  protected void describeMismatchSafely(final String item, final Description mismatchDescription) {
    mismatchDescription.appendText(" was ").appendText(asString(read(item)));
  }

  @Override
  public void describeTo(final Description description) {
    expected.reset();
    description.appendValue(asString(expected));
  }

  private ResultSetRewindable read(final String item) {
    final ByteArrayInputStream data = new ByteArrayInputStream(Payload.encodeUtf8(item));
    return ResultSetFactory.copyResults(ResultSetFactory.load(data, format));
  }

  private String asString(final ResultSet resultSet) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ResultSetFormatter.output(baos, resultSet, format);
    return Payload.decodeUtf8(baos.toByteArray());
  }

}
