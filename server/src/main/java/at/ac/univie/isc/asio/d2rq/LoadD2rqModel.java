package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.InvalidUsage;
import at.ac.univie.isc.asio.tool.Pretty;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.vocabulary.RDF;
import org.d2rq.D2RQException;
import org.d2rq.vocab.D2RConfig;
import org.d2rq.vocab.VocabularySummarizer;
import org.openjena.atlas.AtlasException;
import org.openjena.riot.RiotException;
import org.openjena.riot.system.JenaReaderTurtle2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.MalformedInputException;

/**
 * Read raw rdf file contents into a jena {@code Model}. Resolve relative IRIs using the value of an
 * embedded {@code d2r:baseURI} if available.
 */
public final class LoadD2rqModel {
  /**
   * Create a loader, that will use an embedded base uri or
   * {@link D2rqTools#DEFAULT_BASE} if none is present.
   *
   * @return d2rq model loader
   */
  public static LoadD2rqModel inferBaseUri() {
    return new LoadD2rqModel(Optional.<String>absent());
  }

  /**
   * Create a loader, that will use the given URI to resolve relative IRIs and replaces an embedded
   * base uri.
   *
   * @param override alternative base uri that should be used
   * @return d2rq model loader
   */
  public static LoadD2rqModel overrideBaseUri(final URI override) {
    return new LoadD2rqModel(Optional.of(override.toString()));
  }

  private LoadD2rqModel(final Optional<String> baseUri) {
    this.baseUri = baseUri;
  }

  private final RDFReader parser = new JenaReaderTurtle2();
  private final VocabularySummarizer validator = new VocabularySummarizer(D2RConfig.class);
  private final Optional<String> baseUri;

  /**
   * Resolving relative IRIs may require two parse passes, first to determine the embedded base URI,
   * then a second pass, using the found base URI.
   *
   * @param source raw turtle content
   * @return parsed model
   */
  public Model parse(final ByteSource source) {
    try {
      final Model model = doParse(source);
      validator.assertNoUndefinedTerms(model,
          D2RQException.CONFIG_UNKNOWN_PROPERTY, D2RQException.CONFIG_UNKNOWN_CLASS);
      return model;
    } catch (IOException | JenaException | AtlasException | IllegalArgumentException e) {
      throw new RdfParseError(source.toString(), unwrap(e));
    }
  }

  private Model doParse(final ByteSource source) throws IOException {
    // first pass : use default or overriding to avoid non-deterministic resolution
    Model model = readFrom(source, baseUri.or(D2rqTools.DEFAULT_BASE));
    final Optional<String> embeddedBaseUri = D2rqTools.findEmbeddedBaseUri(model);
    if (!baseUri.isPresent() && embeddedBaseUri.isPresent()) {
      // second pass : use retrieved base uri for resolution if no override set
      model.close();
      model = readFrom(source, embeddedBaseUri.get());
    }
    injectBaseUri(model, baseUri.or(embeddedBaseUri.or(D2rqTools.DEFAULT_BASE)));
    return model;
  }

  /**
   * Replace any existing {@link org.d2rq.vocab.D2RConfig#baseURI} with the given.
   * If no {@link org.d2rq.vocab.D2RConfig#Server} is present, create one and attach the given uri.
   *
   * @param model   given configuration
   * @param baseUri uri that should be injected
   * @return modified model
   */
  static Model injectBaseUri(final Model model, final String baseUri) {
    final Optional<Resource> maybeServer = D2rqTools.findSingleOfType(model, D2RConfig.Server);
    final Resource server;
    if (maybeServer.isPresent()) {
      server = maybeServer.get();
    } else {  // create an empty one
      server = model.createResource().addProperty(RDF.type, D2RConfig.Server);
    }
    server.removeAll(D2RConfig.baseURI);
    server.addProperty(D2RConfig.baseURI, model.createResource(baseUri));
    return model;
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

  public static class RdfParseError extends InvalidUsage {
    public RdfParseError(final String source, final Throwable cause) {
      super(Pretty.format("failed to parse <%s> : %s", source, cause), cause);
    }
  }
}
