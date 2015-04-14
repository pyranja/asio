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
