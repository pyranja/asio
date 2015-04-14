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
