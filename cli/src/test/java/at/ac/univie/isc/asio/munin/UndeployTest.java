/*
 * #%L
 * asio cli
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
package at.ac.univie.isc.asio.munin;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.Pigeon;
import at.ac.univie.isc.asio.munin.Undeploy;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.StringWriter;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class UndeployTest {
  private final Pigeon pigeon = Mockito.mock(Pigeon.class);
  private final StringWriter sink = new StringWriter();
  private final Undeploy subject = new Undeploy(sink, pigeon);

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_if_required_argument_missing() throws Exception {
    subject.call(Collections.<String>emptyList());
  }
  
  @Test
  public void should_invoke_with_first_positional_argument_as_target() throws Exception {
    subject.call(Collections.singletonList("test"));
    verify(pigeon).undeploy(Id.valueOf("test"));
  }

  @Test
  public void should_print_success() throws Exception {
    given(pigeon.undeploy(Id.valueOf("test"))).willReturn(true);
    final int code = subject.call(Collections.singletonList("test"));
    assertThat(sink.toString(), containsString("'test' undeployed"));
    assertThat("exit code", code, equalTo(0));
  }

  @Test
  public void should_print_not_found() throws Exception {
    given(pigeon.undeploy(Id.valueOf("test"))).willReturn(false);
    final int code = subject.call(Collections.singletonList("test"));
    assertThat(sink.toString(), containsString("no container named 'test' present"));
    assertThat("exit code", code, equalTo(1));
  }
}
