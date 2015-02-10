package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.io.Payload;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import org.d2rq.vocab.D2RConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;

import static at.ac.univie.isc.asio.junit.IsIsomorphic.isomorphicWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoadD2rqModelTest {
  public static final Resource THING = ResourceFactory.createResource("http://example.org/thing");
  @Rule
  public final TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public final ExpectedException error = ExpectedException.none();

  private final Model expected = ModelFactory.createDefaultModel();
  private final LoadD2rqModel subject = new LoadD2rqModel();

  @Test
  public void read_triple_from_turtle_file() throws Exception {
    final ByteSource turtle =
        dump(Payload.encodeUtf8("<http://example.org/it> a <http://example.org/thing> ."));
    final Model result = subject.parse(turtle);
    expected.createResource("http://example.org/it").addProperty(RDF.type, THING);
    assertThat(result, is(isomorphicWith(expected)));
  }

  @Test
  public void resolve_relative_resource_iris_with_default_base() throws Exception {
    final ByteSource turtle = dump(Payload.encodeUtf8("<it> a <http://example.org/thing> ."));
    final Model result = subject.parse(turtle);
    expected.createResource(D2rqSpec.DEFAULT_BASE + "it").addProperty(RDF.type, THING);
    assertThat(result, is(isomorphicWith(expected)));
  }

  @Test
  public void resolve_relative_iris_with_embedded_baseURI() throws Exception {
    final ByteSource turtle = dump(Payload.encodeUtf8(
        "<urn:d2r:server> a <http://sites.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/config.rdf#Server> ;"
            + "<http://sites.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/config.rdf#baseURI> <http://test.com/> ."
            + "<it> a <http://example.org/thing> ."));
    final Model result = subject.parse(turtle);
    expected.createResource("urn:d2r:server")
        .addProperty(D2RConfig.baseURI, expected.createResource("http://test.com/"))
        .addProperty(RDF.type, D2RConfig.Server);
    expected.createResource("http://test.com/it").addProperty(RDF.type, THING);
    assertThat(result, is(isomorphicWith(expected)));
  }

  @Test
  public void show_source_description_in_error() throws Exception {
    final ByteSource invalid = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        throw new IOException("test");
      }

      @Override
      public String toString() {
        return "test-source";
      }
    };
    error.expect(LoadD2rqModel.RdfParseError.class);
    error.expectMessage(containsString(invalid.toString()));
    subject.parse(invalid);
  }

  @Test
  public void explain_illegal_d2rq_config_syntax() throws Exception {
    final ByteSource turtle = dump(Payload.encodeUtf8(
        "<urn:d2r:invalid> a <http://sites.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/config.rdf#ILLEGAL> ."
    ));
    error.expectMessage(
        containsString("Unknown class <http://sites.wiwiss.fu-berlin.de/suhl/bizer/d2r-server/config.rdf#ILLEGAL>"));
    subject.parse(turtle);
  }

  @Test
  public void explain_illegal_format_of_file_content() throws Exception {
    final ByteSource notTurtle = dump(Payload.encodeUtf8("not a turtle file"));
    error.expectMessage(containsString("RiotException"));
    subject.parse(notTurtle);
  }

  @Test
  public void explain_encoding_error() throws Exception {
    // <ä> a <ö> . | illegal byte sequence 0xC1 injected before ä
    final byte[] malformedUtf8 = new byte[] {0x3C, (byte) 0xC1, (byte) 0xC3, (byte) 0xA4, 0x3E,
        0x20, 0x61, 0x20, 0x3C, (byte) 0xC3, (byte) 0xB6, 0x3E, 0x20, 0x2E};
    final ByteSource malformed = dump(malformedUtf8);
    error.expectMessage(containsString("MalformedInputException"));
    subject.parse(malformed);
  }

  private ByteSource dump(final byte[] content) throws IOException {
    return ByteSource.wrap(content);
  }
}
