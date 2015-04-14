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
