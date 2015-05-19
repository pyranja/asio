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
package at.ac.univie.isc.asio.tool;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class MediaTypeSerializerTest {
  private final StringWriter sink;
  private final JsonGenerator generator;

  private final MediaTypeSerializer subject;

  public MediaTypeSerializerTest() throws IOException {
    sink = new StringWriter();
    generator = new JsonFactory().createGenerator(sink);
    subject = new MediaTypeSerializer();
  }

  @Test
  public void should_write_type_as_first_part_of_value() throws Exception {
    final MediaType input = new MediaType("text", "test");
    assertThat(serialize(input), startsWith("\"text/"));
  }

  @Test
  public void should_write_sub_type_as_second_part_of_value() throws Exception {
    final MediaType input = new MediaType("text", "test");
    assertThat(serialize(input), endsWith("/test\""));
  }

  @Test
  public void should_omit_mime_properties() throws Exception {
    final MediaType input =
        new MediaType("text", "test", Collections.singletonMap("param", "value"));
    assertThat(serialize(input), equalTo("\"text/test\""));
  }

  @Test
  public void should_omit_charset_parameter() throws Exception {
    final MediaType input = MediaType.APPLICATION_JSON_TYPE.withCharset("utf-8");
    assertThat(serialize(input), equalTo("\"application/json\""));
  }

  @Test
  public void should_write_wildcard_type() throws Exception {
    assertThat(serialize(MediaType.WILDCARD_TYPE), equalTo("\"*/*\""));
  }

  private String serialize(final MediaType input) throws IOException {
    subject.serialize(input, generator, null);
    generator.flush();
    return sink.toString();
  }
}
