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

import com.google.common.net.MediaType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Locale;

final class MimeMatcher extends TypeSafeMatcher<String> {
  private static final String WILDCARD = "*";

  private final MediaType expected;

  public MimeMatcher(final MediaType expected) {
    this.expected = expected;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(expected);
  }

  @Override
  protected boolean matchesSafely(final String item) {
    final MediaType actual = MediaType.parse(item.toLowerCase(Locale.ENGLISH));
    return mainTypeCompatible(actual) && subTypeCompatible(actual) && parametersCompatible(actual);
  }

  private boolean parametersCompatible(final MediaType actual) {
    return actual.parameters().entries().containsAll(expected.parameters().entries());
  }

  private boolean subTypeCompatible(final MediaType actual) {
    return (expected.subtype().equals(WILDCARD) || actual.subtype().equals(expected.subtype()) || actual.subtype().endsWith("+" + expected.subtype()));
  }

  private boolean mainTypeCompatible(final MediaType actual) {
    return expected.type().equals(WILDCARD) || actual.type().equals(expected.type());
  }
}
