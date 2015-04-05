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
