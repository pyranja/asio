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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * Serialize {@link MediaType JAX-RS mime types} to their simple name, omitting all parameters.
 * This serialization is lossy, as the omitted parameters cannot be reconstructed.
 */
public final class MediaTypeSerializer extends StdScalarSerializer<MediaType> {
  public MediaTypeSerializer() {
    super(MediaType.class);
  }

  @Override
  public void serialize(final MediaType value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
    jgen.writeString(value.getType() + "/" + value.getSubtype());
  }
}
