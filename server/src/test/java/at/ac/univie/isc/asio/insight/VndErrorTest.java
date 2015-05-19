/*
 * #%L
 * asio server
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
package at.ac.univie.isc.asio.insight;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.ClassSanityTester;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class VndErrorTest {
  @Test
  public void sane_class() throws Exception {
    new ClassSanityTester()
        .setSampleInstances(Throwable.class, Arrays.asList(new RuntimeException("exception-one"), new RuntimeException("exception-two")))
        .setSampleInstances(Correlation.class, Arrays.asList(Correlation.valueOf("one"), Correlation.valueOf("two")))
        .forAllPublicStaticMethods(VndError.class)
        .testEquals()
    ;
  }

  @Test
  public void jackson_round_tripping() throws Exception {
    final Correlation correlation = Correlation.valueOf("correlation");
    final VndError.ErrorChainElement first =
        VndError.ErrorChainElement.create("first-exception", "first-location");
    final VndError.ErrorChainElement second =
        VndError.ErrorChainElement.create("second-exception", "second-location");
    final VndError original =
        VndError.create("message", "cause", correlation, 1337, ImmutableList.of(first, second));
    final ObjectMapper mapper = new ObjectMapper();
    final String json = mapper.writeValueAsString(original);
    final VndError read = mapper.readValue(json, VndError.class);
    assertThat(read, is(original));
  }

  @Test(timeout = 1_000L)
  public void do_not_fail_on_circular_causal_chain() throws Exception {
    final RuntimeException top = new RuntimeException("top");
    final RuntimeException circular = new RuntimeException("circular");
    top.initCause(circular);
    circular.initCause(top);
    VndError.from(top, Correlation.valueOf("none"), -1L, true);
  }
}
