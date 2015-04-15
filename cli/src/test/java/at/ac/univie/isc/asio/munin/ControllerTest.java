package at.ac.univie.isc.asio.munin;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ControllerTest {
  private final Map<String, Command> commands = new HashMap<>();
  private final Appendable sink = new StringWriter();
  private final Controller subject = new Controller(sink, commands);
  private final TestCommand command = new TestCommand();

  @Before
  public void mapTestCommand() {
    commands.put("test", command);
  }

  // === error handling

  @Test
  public void should_exit_with_code__1__on_exception() throws Exception {
    try {
      subject.run(null);
    } catch (Exception ignored) {}
    assertThat(subject.getExitCode(), equalTo(1));
  }

  @Test
  public void should_exit_with_code__1__and_print_error_on_failed_command() throws Exception {
    commands.put("fail", new Command() {
      @Override
      public int call(final List<String> arguments) throws IOException {
        throw new IllegalStateException("reason");
      }
    });
    try {
      subject.dispatch("fail", Collections.<String>emptyList());
    } catch (Exception ignored) {}
    assertThat(subject.getExitCode(), equalTo(1));
    assertThat(sink.toString(), containsString("'fail' failed - reason"));
  }

  // === usage

  @Test
  public void should_print_mapped_commands_in_usage() throws Exception {
    commands.put("illegal", null);
    final String message = subject.usage();
    assertThat(message, stringContainsInOrder(Arrays.asList("test", ":", command.toString())));
    assertThat(message, stringContainsInOrder(Arrays.asList("illegal", ":", "null")));
  }

  // === command line parsing

  @Test
  public void should_use_null_name_and_no_arguments_if_no_arguments_present() throws Exception {
    subject.parse();
    assertThat(subject.getCommandName(), nullValue());
    assertThat(subject.getCommandArguments(), empty());
  }

  @Test
  public void should_use_first_positional_as_command_name() throws Exception {
    subject.parse("first", "second");
    assertThat(subject.getCommandName(), equalTo("first"));
  }

  @Test
  public void should_use_remaining_after_first_as_command_arguments() throws Exception {
    subject.parse("name", "first-arg", "second-arg");
    assertThat(subject.getCommandArguments(), contains("first-arg", "second-arg"));
  }

  @Test
  public void should_leave_command_arguments_empty_if_only_name_given() throws Exception {
    subject.parse("name");
    assertThat(subject.getCommandArguments(), empty());
  }

  @Test
  public void should_ignore_interleaved_options() throws Exception {
    subject.parse("-opt", "--opt", "name", "-opt=test", "first-arg", "second-arg", "--opt:colon");
    assertThat(subject.getCommandName(), equalTo("name"));
    assertThat(subject.getCommandArguments(), contains("first-arg", "second-arg"));
  }

  @Test
  public void should_lower_case_command_name() throws Exception {
    subject.parse("nAmE");
    assertThat(subject.getCommandName(), equalTo("name"));
  }

  // === dispatch

  @Test
  public void should_print_usage_on_command__help__() throws Exception {
    subject.dispatch("help", Collections.<String>emptyList());
    assertThat(sink.toString(), containsString("### usage:"));
  }

  @Test
  public void should_print_error_and_usage_on_missing_command() throws Exception {
    subject.dispatch(null, Collections.<String>emptyList());
    assertThat(sink.toString(), stringContainsInOrder(Arrays.asList("missing command", "### usage:")));
  }

  @Test
  public void should_print_error_and_usage_on_unknown_command() throws Exception {
    subject.dispatch("unknown", Collections.<String>emptyList());
    assertThat(sink.toString(), stringContainsInOrder(Arrays.asList("unknown command 'unknown'", "### usage:")));
  }

  @Test
  public void should_exit_with_zero_code_on_command__help__() throws Exception {
    subject.dispatch("help", Collections.<String>emptyList());
    assertThat(subject.getExitCode(), equalTo(0));
  }

  @Test
  public void should_exit_with_code__2__on_missing_command() throws Exception {
    subject.dispatch(null, Collections.<String>emptyList());
    assertThat(subject.getExitCode(), equalTo(2));
  }

  @Test
  public void should_exit_with_code__2__on_unknown_command() throws Exception {
    subject.dispatch("unknown", Collections.<String>emptyList());
    assertThat(subject.getExitCode(), equalTo(2));
  }

  @Test
  public void should_exit_with_code_from_invoked_command() throws Exception {
    command.code.set(10);
    subject.dispatch("test", Collections.<String>emptyList());
    assertThat(subject.getExitCode(), equalTo(10));
  }

  @Test
  public void should_invoke_requested_command() throws Exception {
    subject.dispatch("test", Collections.<String>emptyList());
    assertThat(command.called.get(), equalTo(true));
  }

  private static class TestCommand implements Command {
    final AtomicBoolean called;
    final AtomicInteger code = new AtomicInteger(0);
    List<String> captured = null;

    public TestCommand() {
      this.called = new AtomicBoolean(false);
    }

    @Override
    public int call(List<String> args) {
      called.set(true);
      captured = args;
      return code.get();
    }
  }
}
