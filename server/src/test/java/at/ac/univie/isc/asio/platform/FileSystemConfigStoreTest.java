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
package at.ac.univie.isc.asio.platform;

import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.io.TransientPath;
import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static at.ac.univie.isc.asio.io.PathMatchers.aDirectory;
import static at.ac.univie.isc.asio.io.PathMatchers.aFile;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FileSystemConfigStoreTest {
  @Rule
  public ExpectedException error = ExpectedException.none();
  @Rule
  public TransientPath root = TransientPath.folder();

  private FileSystemConfigStore subject;

  @Before
  public void setUp() throws Exception {
    subject = new FileSystemConfigStore(root.path(), Timeout.undefined());
  }

  @Test
  public void should_create_marker_file_on_initialization() throws Exception {
    assertThat("marker file not written", subject.getRoot().resolve(".asio"), aFile());
  }

  @Test
  public void should_create_root_path_on_initialization() throws Exception {
    final Path folder = root.path().resolve("sub-folder");
    assertThat("sub-folder already existed", folder, not(aDirectory()));
    new FileSystemConfigStore(folder, Timeout.undefined());
    assertThat(folder, aDirectory());
  }

  @Test
  public void should_use_a_folder_inside_configured_root_path() throws Exception {
    assertThat("store folder outside of configured root", subject.getRoot().startsWith(root.path()), is(true));
    assertThat("store folder equal to configured root", subject.getRoot(), not(equalTo(root.path())));
  }

  @Test
  public void should_yield_an_empty_mapping_if_no_items_stored_currently() throws Exception {
    assertThat(subject.findAllWithIdentifier("test").keySet(), empty());
  }

  @Test
  public void should_reject_illegal_identifier() throws Exception {
    error.expect(InvalidUsage.class);
    subject.findAllWithIdentifier(null);
  }

  @Test
  public void should_find_present_config_items() throws Exception {
    Files.write(subject.getRoot().resolve("first##test"), Payload.randomWithLength(24));
    Files.write(subject.getRoot().resolve("second##test"), Payload.randomWithLength(24));
    assertThat(subject.findAllWithIdentifier("test").keySet(), containsInAnyOrder("first", "second"));
  }

  @Test
  public void should_find_only_items_with_correct_identifier() throws Exception {
    Files.write(subject.getRoot().resolve("first##other"), Payload.randomWithLength(24));
    Files.write(subject.getRoot().resolve("second##test"), Payload.randomWithLength(24));
    assertThat(subject.findAllWithIdentifier("test").keySet(), containsInAnyOrder("second"));
  }

  @Test
  public void should_yield_ByteSource_ref_to_correct_content() throws Exception {
    final byte[] firstContent = Payload.randomWithLength(1024);
    Files.write(subject.getRoot().resolve("first##test"), firstContent);
    final byte[] secondContent = Payload.randomWithLength(256);
    Files.write(subject.getRoot().resolve("second##test"), secondContent);
    final Map<String, ByteSource> found = subject.findAllWithIdentifier("test");
    assertThat(found.get("first").read(), equalTo(firstContent));
    assertThat(found.get("second").read(), equalTo(secondContent));
  }

  @Test
  public void should_not_find_directories() throws Exception {
    Files.createDirectories(subject.getRoot().resolve("directory##test"));
    assertThat(subject.findAllWithIdentifier("test").keySet(), empty());
  }

  @Test
  public void should_reject_identifier_with_illegal_characters() throws Exception {
    error.expect(InvalidUsage.class);
    subject.findAllWithIdentifier("123##test");
  }

  @Test
  public void should_create_saved_file_at_returned_path() throws Exception {
    final URI saved = subject.save("test", "file.tmp", ByteSource.empty());
    assertThat(Paths.get(saved), aFile());
  }

  @Test
  public void should_yield_references_that_are_readable_as_file_url() throws Exception {
    final byte[] data = Payload.randomWithLength(2050);
    final URI saved = subject.save("test", "file.tmp", ByteSource.wrap(data));
    assertThat(saved.toString(), startsWith("file:///"));
    final URL url = saved.toURL();
    assertThat(url.getProtocol(), equalTo("file"));
    try (final InputStream fileStream = url.openStream()) {
      final byte[] content = ByteStreams.toByteArray(fileStream);
      assertThat("reading through url yielded different content", content, equalTo(data));
    }
  }

  @Test
  public void should_write_content_to_saved_file() throws Exception {
    final byte[] data = Payload.randomWithLength(2050);
    final URI saved = subject.save("test", "file.data", ByteSource.wrap(data));
    assertThat("saved wrong content", Files.readAllBytes(Paths.get(saved)), equalTo(data));
  }

  @Test
  public void should_save_as_file_with_given_name_as_suffix() throws Exception {
    final URI saved = subject.save("test", "file.suffix", ByteSource.empty());
    assertThat(saved.toString(), endsWith("file.suffix"));
  }

  @Test
  public void should_overwrite_existing_files() throws Exception {
    subject.save("test", "file.data", ByteSource.wrap(Payload.randomWithLength(2040)));
    final byte[] content = Payload.randomWithLength(2020);
    final URI saved = subject.save("test", "file.data", ByteSource.wrap(content));
    assertThat("saved wrong content", Files.readAllBytes(Paths.get(saved)), equalTo(content));
  }

  @Test
  public void should_reject_unsafe_qualifier_with_path_separator() throws Exception {
    error.expect(InvalidUsage.class);
    subject.save("illegal" + File.pathSeparator + "qualifier", ".legal", ByteSource.empty());
  }

  @Test
  public void should_reject_unsafe_qualifier_with_separator() throws Exception {
    error.expect(InvalidUsage.class);
    subject.save("illegal" + File.separator + "qualifier", ".legal", ByteSource.empty());
  }

  @Test
  public void should_reject_unsafe_name_with_path_separator() throws Exception {
    error.expect(InvalidUsage.class);
    subject.save("test", "illegal" + File.pathSeparator + "path", ByteSource.empty());
  }

  @Test
  public void should_reject_unsafe_name_with_separator() throws Exception {
    error.expect(InvalidUsage.class);
    subject.save("test", "illegal" + File.separator + "path", ByteSource.empty());
  }

  @Test
  public void should_reject_unsafe_qualifier_with_hash_pound() throws Exception {
    error.expect(InvalidUsage.class);
    subject.save("illegal##qualifier", "test", ByteSource.empty());
  }

  @Test
  public void should_reject_unsafe_name_with_hash_pound() throws Exception {
    error.expect(InvalidUsage.class);
    subject.save("test", "illegal##path", ByteSource.empty());
  }

  @Test
  public void should_not_fail_on_clearing_unknown_qualifier() throws Exception {
    subject.clear("test");
  }

  @Test
  public void should_delete_all_saved_files_of_cleared_qualifier() throws Exception {
    final URI first = subject.save("test", "first", ByteSource.empty());
    final URI second = subject.save("test", "second", ByteSource.empty());
    subject.clear("test");
    assertThat(Paths.get(first), not(aFile()));
    assertThat(Paths.get(second), not(aFile()));
  }

  @Test
  public void should_reject_unsafe_qualifier_with_glob_star() throws Exception {
    error.expect(InvalidUsage.class);
    subject.clear("*");
  }

  @Test
  public void should_not_delete_saved_files_of_qualifiers_with_shared_prefix() throws Exception {
    // this is a possible attack vector
    subject.save("shared", "file", ByteSource.empty());
    final URI mustRemain = subject.save("sharedButNotSame", "file", ByteSource.empty());
    assertThat(Paths.get(mustRemain), aFile());
    subject.clear("shared");
    assertThat(Paths.get(mustRemain), aFile());
  }
}
