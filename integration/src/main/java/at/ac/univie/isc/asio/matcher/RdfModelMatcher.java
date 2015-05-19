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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.openjena.riot.Lang;

import java.io.StringReader;

class RdfModelMatcher extends TypeSafeMatcher<String> {
  private final Model expected;
  private final Lang format;

  RdfModelMatcher(final Model expected, final Lang format) {
    this.expected = expected;
    this.format = format;
  }

  @Override
  protected boolean matchesSafely(final String item) {
    final Model actual = ModelFactory.createDefaultModel();
    actual.read(new StringReader(item), null, format.getName());
    return expected.isIsomorphicWith(actual);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(expected);
  }
}
