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
package at.ac.univie.isc.asio.tool;

import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.io.TransientPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class PropertyHierarchyTest {
  private final PropertyHierarchy subject = new PropertyHierarchy();

  @Test
  public void should_read_properties_file_from_classpath() throws Exception {
    subject.loadEmbedded("PropertyHierarchyTest.properties");
    assertThat(subject.get(), has("key", "value"));
  }

  @Test
  public void should_include_system_properties() throws Exception {
    System.setProperty("sys-key", "sys-val");
    subject.loadSystem();
    assertThat(subject.get(), has("sys-key", "sys-val"));
  }

  @Test
  public void should_include_environment() throws Exception {
    subject.loadSystem();
    for (Map.Entry<String, String> each : System.getenv().entrySet()) {
      assertThat(subject.get(), has(each.getKey(), each.getValue()));
    }
  }

  @Rule
  public final TransientPath external = TransientPath.file(Payload.encodeUtf8("external-key=external-value"));

  @Test
  public void should_read_external_properties_file_from_absolute_path() throws Exception {
    subject.loadExternal(external.path().toString());
    assertThat(subject.get(), has("external-key", "external-value"));
  }

  @Test
  public void should_read_external_properties_from_relative_path() throws Exception {
    subject.single(PropertyHierarchy.EXTERNAL_LOCATION_PROPERTY, external.path().getParent().toString());
    subject.loadExternal(external.path().getFileName().toString());
    assertThat(subject.get(), has("external-key", "external-value"));
  }

  @Test
  public void should_not_fail_if_external_properties_file_not_found() throws Exception {
    subject.loadExternal("does-not-exists.gaga");
    assertThat(subject.get().values(), empty());
  }

  @Test
  public void should_read_cli_kv_agument() throws Exception {
    subject.parseCommandLine(new String[] { "--key=value" });
    assertThat(subject.get(), has("key", "value"));
  }

  @Test
  public void should_read_cli_toggle_argument() throws Exception {
    subject.parseCommandLine(new String[] { "--toggle" });
    assertThat(subject.get(), has("toggle", "true"));
  }

  @Test
  public void should_ignore_positional_arguments() throws Exception {
    subject.parseCommandLine(new String[] { "key=value" });
    assertThat(subject.get().values(), empty());
  }

  private Matcher<Map<?, ?>> has(final String key, final String value) {
    return Matchers.<Object, Object>hasEntry(key, value);
  }
}
