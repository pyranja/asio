package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.Timeout;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.vocab.D2RConfig;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * Wrap a d2rq configuration in rdf format and expose its attributes.
 */
public final class D2rqConfigModel {
  /**
   * Wrap the given d2rq configuration model.
   *
   * @param model d2rq configuration in rdf format
   * @return wrapped configuration
   */
  public static D2rqConfigModel wrap(final Model model) {
    return new D2rqConfigModel(model);
  }

  private final PrefixMapping prefixes;
  private final Model model;
  private final Mapping mapping;

  private D2rqConfigModel(final Model model) {
    this.model = model;
    prefixes = PrefixMapping.Factory.create()
        .withDefaultMappings(PrefixMapping.Extended)
        .setNsPrefixes(model);
    mapping = readMapping(model);
  }

  private Mapping readMapping(final Model model) {
    final String base = D2rqTools.findEmbeddedBaseUri(model).or(D2rqTools.DEFAULT_BASE);
    final Mapping mapping = new MapParser(model, base).parse();
    mapping.configuration().setUseAllOptimizations(true);
    return mapping;
  }

  /**
   * Create a d2rq jena model from the wrapped configuration. The given database connection is
   * injected and overrides internal settings. Each invocation creates a fresh model.
   *
   * @param connection connection to be used
   * @return initialized model, independent from others
   */
  public Model compile(final ConnectedDB connection) {
    return D2rqTools.compile(readMapping(model), connection);
  }

  /**
   * The global identifier of the dataset.
   *
   * @return dataset identifier
   */
  public URI getIdentifier() {
    return asUri(requireServer().getURI(), D2RConfig.Server);
  }

  /**
   * The (absolute) uri used to resolve relative resource names.
   *
   * @return the base uri
   */
  public URI getBaseUri() {
    return asUri(D2rqTools.findEmbeddedBaseUri(model).or(D2rqTools.DEFAULT_BASE), D2RConfig.baseURI);
  }

  /**
   * The prefixes used in this configuration.
   *
   * @return prefix->uri mappings
   */
  public PrefixMapping getPrefixes() {
    return prefixes;
  }

  /**
   * The mapping definition as a RDF model. Sensitive information (e.g. database credentials) are
   * omitted.
   *
   * @return the mapping definition
   */
  public Model getDefinition() {
    final Model cleaned = ModelFactory.createDefaultModel();
    cleaned.add(model);
    cleaned.setNsPrefixes(model.getNsPrefixMap());
    final ResIterator databases = cleaned.listResourcesWithProperty(RDF.type, D2RQ.Database);
    while (databases.hasNext()) {
      final Resource next = databases.next();
      cleaned.removeAll(next, null, null);
      cleaned.removeAll(null, null, next);
    }
    cleaned.createResource(D2RQ.Database);
    return cleaned;
  }

  /**
   * The d2rq mapping rules read from the wrapped configuration.
   *
   * @return the d2rq mapping
   */
  Mapping getMapping() {
    return mapping;
  }

  /**
   * The jdbc connection settings from the wrapped configuration.
   *
   * @return jdbc configuration model
   */
  public D2rqJdbcModel getJdbcConfig() {
    return D2rqJdbcModel.parse(mapping);
  }

  /**
   * Maximum duration of operations on this dataset. Undefined if missing.
   *
   * @return timeout
   */
  public Timeout getTimeout() {
    final Resource server = requireServer();
    return Optional.fromNullable(server.getProperty(D2RConfig.sparqlTimeout))
        .transform(new Function<Statement, Timeout>() {
          @Nullable
          @Override
          public Timeout apply(final Statement input) {
            final RDFNode value = input.getObject();
            try {
              return Timeout.from(value.asLiteral().getLong(), TimeUnit.SECONDS);
            } catch (Exception cause) {
              throw new InvalidD2rqConfig(D2RConfig.sparqlTimeout,
                  "[" + value + "] is not numeric");
            }
          }
        }).or(Timeout.undefined());
  }

  /**
   * Whether this dataset supports SPARQL federated queries.
   *
   * @return true if federated queries are supported
   */
  public boolean isFederationEnabled() {
    return D2rqTools.findSingleOfType(model, SparqlServiceDescription.Service).transform(new Function<Resource, Boolean>() {
      @Nullable
      @Override
      public Boolean apply(final Resource input) {
        return input.hasProperty(SparqlServiceDescription.feature, SparqlServiceDescription.BasicFederatedQuery);
      }
    }).or(false);
  }

  /**
   * Find the server resource, failing fast if it is missing.
   */
  private Resource requireServer() {
    final Optional<Resource> server = D2rqTools.findSingleOfType(model, D2RConfig.Server);
    if (!server.isPresent()) { throw new InvalidD2rqConfig(D2RConfig.Server, "missing"); }
    return server.get();
  }

  /**
   * Convert a string to an URI. If the string cannot be converted fail and indicate that the given
   * source rdf node is illegal.
   */
  private URI asUri(final String raw, final RDFNode source) {
    try {
      return new URI(raw);
    } catch (NullPointerException | URISyntaxException e) {
      throw new InvalidD2rqConfig(source, "<" + raw +"> is not a valid uri (" + e.getMessage() + ")");
    }
  }
}
