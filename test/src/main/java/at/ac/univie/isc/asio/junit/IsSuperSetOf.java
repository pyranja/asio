/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.junit;

import com.hp.hpl.jena.rdf.model.Model;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public final class IsSuperSetOf extends TypeSafeMatcher<Model> {
  /**
   * Create a matcher for {@link com.hp.hpl.jena.rdf.model.Model jena rdf models}, matching when the
   * examined model contains all statements in the given reference model.
   *
   * @param expected expected RDF statements
   */
  public static IsSuperSetOf superSetOf(final Model expected) {
    return new IsSuperSetOf(expected);
  }

  private final Model expected;

  private IsSuperSetOf(final Model expected) {
    this.expected = expected;
  }

  @Override
  protected boolean matchesSafely(final Model item) {
    return item.containsAll(expected);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("model containing all statements from ").appendValue(expected);
  }
}
