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

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ExpandingQNameSerializerTest {
  private final StringWriter sink;
  private final JsonGenerator generator;

  private final ExpandingQNameSerializer subject;

  public ExpandingQNameSerializerTest() throws IOException {
    sink = new StringWriter();
    generator = new JsonFactory().createGenerator(sink);
    subject = new ExpandingQNameSerializer();
  }

  @Test
  public void should_write_local_only_qname_as_is() throws Exception {
    subject.serialize(new QName("local-only"), generator, null);
    generator.flush();
    assertThat(sink.toString(), is("\"local-only\""));
  }

  @Test
  public void should_combine_local_part_and_namespace() throws Exception {
    subject.serialize(new QName("http://test.com/", "local"), generator, null);
    generator.flush();
    assertThat(sink.toString(), is("\"http://test.com/local\""));
  }

  @Test
  public void should_ignore_prefix() throws Exception {
    subject.serialize(new QName("http://test.com/", "local", "prefix"), generator, null);
    generator.flush();
    assertThat(sink.toString(), is("\"http://test.com/local\""));
  }
}
