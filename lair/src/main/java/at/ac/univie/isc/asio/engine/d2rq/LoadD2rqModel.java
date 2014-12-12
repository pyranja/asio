package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.shared.JenaException;
import org.d2rq.D2RQException;
import org.d2rq.vocab.D2RConfig;
import org.d2rq.vocab.VocabularySummarizer;
import org.openjena.atlas.AtlasException;
import org.openjena.riot.RiotException;
import org.openjena.riot.system.JenaReaderTurtle2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;

/**
 * Read raw rdf file contents into a jena {@code Model}. Resolve relative IRIs using the value of an
 * embedded {@code d2r:baseURI} if available.
 */
public final class LoadD2rqModel {

  private final RDFReader parser = new JenaReaderTurtle2();
  private final VocabularySummarizer validator = new VocabularySummarizer(D2RConfig.class);

  /**
   * Resolving relative IRIs may require two parse passes, first to determine the embedded base URI,
   * then a second pass, using the found base URI.
   *
   * @param source raw turtle content
   * @return parsed model
   */
  public Model parse(final ByteSource source) {
    try {
      // first pass : use marker urn to avoid non-deterministic default resolution
      Model model = readFrom(source, D2rqSpec.DEFAULT_BASE);
      final String baseUri = D2rqSpec.findEmbeddedBaseUri(model);
      if (isNotDefault(baseUri)) {
        // second pass : use retrieved base uri
        model.close();
        model = readFrom(source, baseUri);
      }
      validator.assertNoUndefinedTerms(model,
          D2RQException.CONFIG_UNKNOWN_PROPERTY, D2RQException.CONFIG_UNKNOWN_CLASS);
      return model;
    } catch (IOException | JenaException | AtlasException | IllegalArgumentException e) {
      throw new RdfParseError(source.toString(), unwrap(e));
    }
  }

  private boolean isNotDefault(final String baseUri) {
    return !D2rqSpec.DEFAULT_BASE.equals(baseUri);
  }

  private Model readFrom(final ByteSource source, final String base) throws IOException {
    try (final InputStream stream = source.openStream()) {
      final Model model = ModelFactory.createDefaultModel();
      parser.read(model, stream, base);
      return model;
    }
  }

  private Throwable unwrap(final Exception e) {
    final Throwable root = Throwables.getRootCause(e);
    if (root instanceof RiotException || root instanceof MalformedInputException) {
      return root;
    } else {
      return e;
    }
  }

  public static class RdfParseError extends IllegalArgumentException {
    public RdfParseError(final String source, final Throwable cause) {
      super(Pretty.format("failed to parse <%s> : %s", source, cause), cause);
    }
  }
}
