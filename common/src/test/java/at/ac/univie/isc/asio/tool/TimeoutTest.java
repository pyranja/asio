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

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TimeoutTest {

  private Timeout subject;

  @Test
  public void should_store_as_nanos() throws Exception {
    subject = Timeout.from(100, TimeUnit.SECONDS);
    assertThat(subject.value(), is(TimeUnit.SECONDS.toNanos(100)));
  }

  @Test
  public void should_convert_stored_to_given_unit() throws Exception {
    subject = Timeout.from(1, TimeUnit.DAYS);
    assertThat(subject.getAs(TimeUnit.HOURS, -1L), is(24L));
  }

  @Test
  public void undefined_yields_fallback() throws Exception {
    subject = Timeout.undefined();
    assertThat(subject.getAs(TimeUnit.DAYS, 100L), is(100L));
  }

  @Test
  public void negative_timeout_value_yields_undefined() throws Exception {
    subject = Timeout.from(-1, TimeUnit.SECONDS);
    assertThat(subject.isDefined(), is(false));
  }

  @Test
  public void positive_timeout_yields_defined() throws Exception {
    subject = Timeout.from(100L, TimeUnit.SECONDS);
    assertThat(subject.isDefined(), is(true));
  }

  @Test
  public void undefined_timeout_transforms_into_default() {
    subject = Timeout.undefined();
    final Timeout alternative = Timeout.from(10, TimeUnit.SECONDS);
    assertThat(subject.orIfUndefined(alternative), is(alternative));
  }

  @Test
  public void defined_timeout_remains_as_is() {
    subject = Timeout.from(20, TimeUnit.DAYS);
    final Timeout alternative = Timeout.from(10, TimeUnit.SECONDS);
    assertThat(subject.orIfUndefined(alternative), is(subject));
  }

  @Test
  public void should_roundtrip_undefined_as_string() throws Exception {
    final Timeout parsed = Timeout.fromString(Timeout.undefined().toString());
    assertThat(parsed, sameInstance(Timeout.undefined()));
  }

  @Test
  public void should_roundtrip_defined_millisecond_timeout_as_string() throws Exception {
    final Timeout original = Timeout.from(100, TimeUnit.MILLISECONDS);
    final Timeout parsed = Timeout.fromString(original.toString());
    assertThat(parsed, equalTo(original));
  }

  @Test
  public void should_lossy_roundtrip_defined_nanosecond_timeout_as_string() throws Exception {
    final Timeout original = Timeout.from(100, TimeUnit.NANOSECONDS);
    final Timeout parsed = Timeout.fromString(original.toString());
    assertThat(parsed, equalTo(Timeout.from(0, TimeUnit.MILLISECONDS)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_on_illegal_syntax() throws Exception {
    Timeout.fromString("test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_on_illegal_value() throws Exception {
    Timeout.fromString("ms");
  }

  @Test
  public void should_yield_undefined_for_negative_value() throws Exception {
    assertThat(Timeout.fromString("-1ms"), equalTo(Timeout.undefined()));
  }

  @Test(expected = NullPointerException.class)
  public void should_fail_on_null_input() throws Exception {
    Timeout.fromString(null);
  }
}
