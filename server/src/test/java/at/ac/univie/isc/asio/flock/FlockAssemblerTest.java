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
package at.ac.univie.isc.asio.flock;

import at.ac.univie.isc.asio.Id;
import at.ac.univie.isc.asio.io.Payload;
import at.ac.univie.isc.asio.tool.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class FlockAssemblerTest {
  public static final ObjectMapper JACKSON = new ObjectMapper();

  private final FlockAssembler subject =
      new FlockAssembler(JACKSON, Timeout.from(23, TimeUnit.DAYS));

  private FlockConfig make(final Id name, final byte[] data) {
    return ((FlockContainer) subject.assemble(name, ByteSource.wrap(data))).configuration();
  }

  @Test
  public void should_use_default_values_if_config_empty() throws Exception {
    final FlockConfig result = make(Id.valueOf("test"), Payload.encodeUtf8("{}"));
    assertThat(result.getName(), equalTo(Id.valueOf("test")));
    assertThat(result.getTimeout(), equalTo(Timeout.from(23, TimeUnit.DAYS)));
    assertThat(result.getIdentifier(), equalTo(URI.create("asio:///flock/")));
  }

  @Test
  public void should_use_provided_configuration_properties() throws Exception {
    final FlockConfig input = new FlockConfig()
        .setName(Id.valueOf("test"))
        .setIdentifier(URI.create("asio:///test/"))
        .setTimeout(Timeout.from(21, TimeUnit.HOURS));
    final FlockConfig result = make(Id.valueOf("test"), JACKSON.writeValueAsBytes(input));
    assertThat(result.getIdentifier(), equalTo(URI.create("asio:///test/")));
    assertThat(result.getTimeout(), equalTo(Timeout.from(21, TimeUnit.HOURS)));
  }

  @Test
  public void should_override_input_name() throws Exception {
    final FlockConfig input = new FlockConfig().setName(Id.valueOf("test"));
    final FlockConfig result = make(Id.valueOf("override"), JACKSON.writeValueAsBytes(input));
    assertThat(result.getName(), equalTo(Id.valueOf("override")));
  }

  @Test(expected = InvalidFlockConfiguration.class)
  public void should_throw_custom_exception_on_parse_error() throws Exception {
    subject.assemble(Id.valueOf("test"), ByteSource.wrap(Payload.encodeUtf8("{\"timeout\":\"illegal\"}")));
  }
}
