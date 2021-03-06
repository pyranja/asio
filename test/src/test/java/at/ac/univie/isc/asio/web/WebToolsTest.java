/*
 * #%L
 * asio test
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
package at.ac.univie.isc.asio.web;

import org.junit.Test;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class WebToolsTest {
  @Test
  public void directory_path_uri_remains_as_is() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("//localhost/directory/"));
    assertThat(actual.getPath(), is("/directory/"));
    assertThat(actual, is(URI.create("//localhost/directory/")));
  }

  @Test
  public void append_slash_if_missing() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("//localhost/directory"));
    assertThat(actual.getPath(), is("/directory/"));
    assertThat(actual, is(URI.create("//localhost/directory/")));
  }

  @Test
  public void set_to_root_path_if_no_path_given() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("//localhost"));
    assertThat(actual.getPath(), is("/"));
    assertThat(actual, is(URI.create("//localhost/")));
  }

  @Test
  public void ignore_opaque_uris() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("mailto:me@example.com"));
    assertThat(actual.getPath(), is(nullValue()));
    assertThat(actual, is(URI.create("mailto:me@example.com")));
  }

  @Test
  public void keep_query_string_at_end_of_uri() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("//localhost/directory?key=value"));
    assertThat(actual.getPath(), is("/directory/"));
    assertThat(actual, is(URI.create("//localhost/directory/?key=value")));
  }

  @Test
  public void keep_fragment_at_end_of_uri() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("//localhost/directory#fragment"));
    assertThat(actual.getPath(), is("/directory/"));
    assertThat(actual, is(URI.create("//localhost/directory/#fragment")));
  }

  @Test
  public void supports_absolute_paths() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("/directory"));
    assertThat(actual.getPath(), is("/directory/"));
    assertThat(actual, is(URI.create("/directory/")));
  }

  @Test
  public void supports_relative_paths() throws Exception {
    final URI actual = WebTools.ensureDirectoryPath(URI.create("directory"));
    assertThat(actual.getPath(), is("directory/"));
    assertThat(actual, is(URI.create("directory/")));
  }
}
