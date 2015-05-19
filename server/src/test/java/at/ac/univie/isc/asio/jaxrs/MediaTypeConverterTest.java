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
package at.ac.univie.isc.asio.jaxrs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MediaTypeConverterTest {
  @Parameter(0)
  public javax.ws.rs.core.MediaType jaxrsType;
  @Parameter(1)
  public com.google.common.net.MediaType guavaType;

  @Parameters(name = "{index}: {0} -> {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {MediaType.APPLICATION_JSON_TYPE, com.google.common.net.MediaType.create("application", "json")}
        , {MediaType.WILDCARD_TYPE, com.google.common.net.MediaType.ANY_TYPE}
        , {new MediaType("application", "*"), com.google.common.net.MediaType.ANY_APPLICATION_TYPE}
        , {MediaType.APPLICATION_XML_TYPE.withCharset("utf-8"), com.google.common.net.MediaType.APPLICATION_XML_UTF_8}
        , {new MediaType("test", "example", ImmutableMap.of("key1", "val1", "key2", "val2")), com.google.common.net.MediaType.create("test", "example").withParameters(ImmutableMultimap.of("key1", "val1", "key2", "val2"))}
        , {null, null}
    });
  }

  @Test
  public void forward_conversion() throws Exception {
    assertThat(MediaTypeConverter.instance().convert(jaxrsType), is(guavaType));
  }

  @Test
  public void reverse_conversion() throws Exception {
    assertThat(MediaTypeConverter.instance().reverse().convert(guavaType), is(jaxrsType));
  }
}
