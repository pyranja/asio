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

import com.google.common.base.Optional;
import org.junit.Test;
import rx.Observable;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ReactiveTest {
  @Test
  public void should_yield_absent_if_source_observable_is_empty() throws Exception {
    assertThat(Reactive.asOptional(Observable.<String>empty()), equalTo(Optional.<String>absent()));
  }

  @Test
  public void should_yield_single_element_of_sequence() throws Exception {
    assertThat(Reactive.asOptional(Observable.just("test")).get(), equalTo("test"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_if_source_sequence_has_multiple_elements() throws Exception {
    Reactive.asOptional(Observable.from("one", "two"));
  }
}
