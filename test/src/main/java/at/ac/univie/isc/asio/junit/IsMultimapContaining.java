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

import com.google.common.collect.Multimap;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;

public final class IsMultimapContaining<K, V> extends TypeSafeMatcher<Multimap<K, V>> {
  private final Matcher<? super K> keyMatcher;
  private final Matcher<? super Collection<? extends V>> valueMatcher;

  private IsMultimapContaining(final Matcher<? super K> keyMatcher, final Matcher<? super Collection<? extends V>> valueMatcher) {
    this.keyMatcher = keyMatcher;
    this.valueMatcher = valueMatcher;
  }

  @Override
  protected boolean matchesSafely(final Multimap<K, V> item) {
    for (Map.Entry<K, Collection<V>> entry : item.asMap().entrySet()) {
      if (keyMatcher.matches(entry.getKey()) && valueMatcher.matches(entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("map containing [")
        .appendDescriptionOf(keyMatcher)
        .appendText("->")
        .appendDescriptionOf(valueMatcher)
        .appendText("]");
  }

  @Override
  protected void describeMismatchSafely(final Multimap<K, V> item,
                                        final Description mismatchDescription) {
    mismatchDescription.appendText("map was ").appendValueList("[", ", ", "]", item.entries());
  }

  /**
   * Creates a matcher for {@link com.google.common.collect.Multimap}s matching when the examined
   * map contains at least one entry whose key satisfies the specified <code>keyMatcher</code>
   * <b>and</b> whose value satisfies the specified <code>valueMatcher</code>.
   * <p/>
   *
   * @param keyMatcher
   *     the key matcher that, in combination with the valueMatcher, must be satisfied by at least one entry
   * @param valueMatcher
   *     the value matcher that, in combination with the keyMatcher, must be satisfied by at least one entry
   */
  @Factory
  public static <K, V> IsMultimapContaining<K, V> hasEntries(
      final Matcher<? super K> keyMatcher,
      final Matcher<? super Collection<? extends V>> valueMatcher) {
    return new IsMultimapContaining<>(keyMatcher, valueMatcher);
  }

  /**
   * Creates a matcher for {@link com.google.common.collect.Multimap}s matching when the examined
   * map contains at least one entry whose key equals the specified <code>key</code> <b>and</b> is
   * associated to <b>all</b> given values.
   * <p/>
   *
   * @param key the key that must be present
   * @param values all values associated with {@code key}
   */
  @SafeVarargs
  @Factory
  public static <K, V> IsMultimapContaining<K, V> hasEntries(final K key,
      final V... values) {
    return new IsMultimapContaining<>(equalTo(key), containsInAnyOrder(values));
  }
}
