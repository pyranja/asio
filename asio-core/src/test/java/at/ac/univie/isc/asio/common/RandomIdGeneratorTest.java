package at.ac.univie.isc.asio.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

public class RandomIdGeneratorTest {

  private IdGenerator subject = RandomIdGenerator.withPrefix("test");

  @Test
  public void consecutive_ids_differ() throws Exception {
    final String first = subject.next();
    final String second = subject.next();
    assertNotEquals(first, second);
  }

  @Test
  public void ids_start_with_set_prefix() throws Exception {
    final String generated = subject.next();
    assertThat(generated, startsWith("test" + RandomIdGenerator.DELIMITER));
  }

  @Test
  public void ids_have_a_non_empty_suffix() throws Exception {
    final String generated = subject.next();
    final String[] parts = generated.split(RandomIdGenerator.DELIMITER, 2);
    assertThat(parts[1], not(isEmptyString()));
  }

  /*
   * UUIDs have 5 blocks separated by '-'
   */

  @Test
  public void id_has_additional_prefix_block() throws Exception {
    final String[] parts = subject.next().split(RandomIdGenerator.DELIMITER);
    assertThat(parts.length, is(6));
  }

  @Test
  public void id_has_only_UUID_blocks_if_without_prefix() throws Exception {
    subject = RandomIdGenerator.withoutPrefix();
    final String[] parts = subject.next().split(RandomIdGenerator.DELIMITER);
    assertThat(parts.length, is(5));
  }

  // integration

  @Test
  public void ids_are_valid_file_names() throws Exception {
    Paths.get(subject.next());
  }

  @Test
  public void ids_are_valid_urls() throws Exception {
    URI.create("http://localhost/" + subject.next());
    new URL("http://localhost/" + subject.next());
  }
}
