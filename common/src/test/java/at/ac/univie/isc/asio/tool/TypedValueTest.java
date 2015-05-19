/*
 * #%L
 * asio common
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
package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.base.Charsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.Nonnull;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class TypedValueTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_be_a_transparent_wrapper() throws Exception {
    class TestType extends TypedValue<String> {
      protected TestType(@Nonnull final String val) {
        super(val);
      }
    }
    final String value = new String(Payload.randomWithLength(256), Charsets.UTF_8);
    final TypedValue<String> subject = new TestType(value);
    assertThat(subject.toString(), is(value));
    final TypedValue<String> other = new TestType(subject.toString());
    assertThat(other, is(equalTo(subject)));
  }

  @Test
  public void should_reject_null_value() throws Exception {
    exception.expect(NullPointerException.class);
    new TypedValue<String>(null) {};
  }

  @Test
  public void subclasses_cannot_be_equal() throws Exception {
    final String value = "test";
    final TypedValue<String> subject = new TypedValue<String>(value) {};
    final TypedValue<String> other = new TypedValue<String>(value) {};
    assertThat(subject, is(not(equalTo(other))));
    assertThat(subject.hashCode(), is(not(equalTo(other.hashCode()))));
  }

  @Test
  public void should_call_normalize() throws Exception {
    final String replacement = "replaced";
    final TypedValue<String> subject = new TypedValue<String>("test") {
      @Nonnull
      @Override
      protected String normalize(@Nonnull final String val) {
        return replacement;
      }
    };
    assertThat(subject.toString(), is(replacement));
  }
}
