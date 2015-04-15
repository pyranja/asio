package at.ac.univie.isc.asio.munin.commands;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.munin.Pigeon;
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
