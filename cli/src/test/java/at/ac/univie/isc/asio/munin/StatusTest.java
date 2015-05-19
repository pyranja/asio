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
import at.ac.univie.isc.asio.ServerStatus;
import at.ac.univie.isc.asio.munin.Status;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

public class StatusTest {
  private final Pigeon pigeon = Mockito.mock(Pigeon.class);
  private final StringWriter sink = new StringWriter();

  private final Status status = new Status(sink, pigeon);

  @Test
  public void scenario_server_up_and_has_active_containers() throws Exception {
    given(pigeon.health()).willReturn(ServerStatus.UP);
    given(pigeon.activeContainer()).willReturn(Arrays.asList(Id.valueOf("one"), Id.valueOf("two")));
    final int code = status.call(Collections.<String>emptyList());
    assertThat("exit code", code, equalTo(0));
    assertThat(sink.toString(),
        stringContainsInOrder(Arrays.asList("server is UP", "active container", "one", "two")));
  }

  @Test
  public void scenario_server_up_and_no_containers_active() throws Exception {
    // either not supported or just none deployed yet
    given(pigeon.health()).willReturn(ServerStatus.UP);
    given(pigeon.activeContainer()).willReturn(Collections.<Id>emptyList());
    final int code = status.call(Collections.<String>emptyList());
    assertThat("exit code", code, equalTo(0));
    assertThat(sink.toString(), equalToIgnoringWhiteSpace("server is UP"));
  }

  @Test
  public void scenario_server_up_and_container_fetching_fails() throws Exception {
    given(pigeon.health()).willReturn(ServerStatus.UP);
    given(pigeon.activeContainer()).willThrow(new IllegalStateException("test"));
    final int code = status.call(Collections.<String>emptyList());
    assertThat("exit code", code, equalTo(0));
    assertThat(sink.toString(), equalToIgnoringWhiteSpace("server is UP"));
  }

  @Test
  public void scenario_server_down() throws Exception {
    given(pigeon.health()).willReturn(ServerStatus.DOWN);
    final int code = status.call(Collections.<String>emptyList());
    assertThat("exit code", code, equalTo(0));
    assertThat(sink.toString(), equalToIgnoringWhiteSpace("server is DOWN"));
  }

  @Test
  public void scenario_health_check_fails() throws Exception {
    given(pigeon.health()).willThrow(new IllegalStateException("test"));
    final int code = status.call(Collections.<String>emptyList());
    assertThat("exit code", code, equalTo(0));
    assertThat(sink.toString(), equalToIgnoringWhiteSpace("server is DOWN"));
  }
}
