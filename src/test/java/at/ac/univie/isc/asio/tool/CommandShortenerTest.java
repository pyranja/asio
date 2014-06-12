package at.ac.univie.isc.asio.tool;

import com.google.common.base.Strings;

import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CommandShortenerTest {

  private CommandShortener subject = new CommandShortener();
  private String text;

  @Test
  public void should_leave_single_line_text_as_is() throws Exception {
    text = "my simple command";
    assertThat(subject.apply(text), is("my simple command"));
  }

  @Test
  public void should_replace_new_lines_with_whitespace() throws Exception {
    text = String.format(Locale.ENGLISH, "my three%nline%ncommand");
    assertThat(subject.apply(text), is("my three line command"));
  }

  @Test
  public void should_strip_trailing_whitespace() throws Exception {
    text = "my command    ";
    assertThat(subject.apply(text), is("my command"));
  }

  @Test
  public void should_strip_leading_whitespace() throws Exception {
    text = "    my command";
    assertThat(subject.apply(text), is("my command"));
  }

  @Test
  public void should_remove_leading_prefixes() throws Exception {
    text = String.format(Locale.ENGLISH, "PREFIX ov: <http://open.vocab.org/terms/>%nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>%nSELECT *%nWHERE { ?s ?p ?o }");
    assertThat(subject.apply(text), is("SELECT * WHERE { ?s ?p ?o }"));
  }

  @Test
  public void should_shorten_long_commands() throws Exception {
    text = Strings.repeat("my command", 20);
    assertThat(text.length(), is(greaterThan(CommandShortener.MAX_LENGTH)));
    assertThat(subject.apply(text).length(), is(CommandShortener.MAX_LENGTH));
  }

  @Test
  public void should_add_truncation_marker_to_shortened_command() throws Exception {
    text = Strings.repeat("my command", 20);
    assertThat(text.length(), is(greaterThan(CommandShortener.MAX_LENGTH)));
    assertThat(subject.apply(text), endsWith(CommandShortener.TRUNCATION_MARKER));
  }
}
