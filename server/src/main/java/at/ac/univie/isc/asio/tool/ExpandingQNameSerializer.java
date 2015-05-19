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

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * Serialize {@link QName qualified names} to their full uri form. This serialization is lossy, as
 * it is not possible to separate the namespace and local part after smashing them together.
 */
public final class ExpandingQNameSerializer extends StdScalarSerializer<QName> {
  public ExpandingQNameSerializer() {
    super(QName.class);
  }

  @Override
  public void serialize(final QName value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
    jgen.writeString(Pretty.expand(value));
  }
}
