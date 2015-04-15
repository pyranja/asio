package at.ac.univie.isc.asio.munin.commands;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.munin.Pigeon;
import at.ac.univie.isc.asio.munin.ServerStatus;
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
