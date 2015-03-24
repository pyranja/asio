package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.Pretty;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import org.d2rq.CompiledMapping;
import org.d2rq.D2RQException;
import org.d2rq.db.SQLConnection;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.D2RQCompiler;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.Mapping;
import org.d2rq.vocab.D2RConfig;
import org.d2rq.vocab.D2RQ;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Capture d2rq settings and mapping.
 */
public final class D2rqSpec {
  /**
   * Default base to resolve relative IRIs
   */
  public static final String DEFAULT_BASE = "asio:///default/";

  /**
   * Wrap the given rdf based d2rq configuration model.
   *
   * @param configuration rdf model of settings
   * @return wrapper
   */
  public static D2rqSpec wrap(final Model configuration) {
    return new D2rqSpec(configuration);
  }

  /**
   * Load and parse the given data as a d2rq mapping file. The given {@code baseUri} overrides any
   * embedded one.
   *
   * @param source  raw configuration data
   * @param baseUri uri used to resolve relative IRIs
   * @return initialized d2rq specification
   */
  public static D2rqSpec load(final ByteSource source, final URI baseUri) {
    final Model configuration = LoadD2rqModel.overrideBaseUri(baseUri).parse(source);
    return D2rqSpec.wrap(configuration);
  }

  /**
   * Find the embedded base resource IRI if present.
   *
   * @param model given configuration
   * @return base resource IRI embedded in model.
   */
  static Optional<String> findEmbeddedBaseUri(final Model model) {
    final Optional<Resource> server = findServerResource(model);
    if (server.isPresent()) {
      final Resource baseUriProperty = server.get().getPropertyResourceValue(D2RConfig.baseURI);
      return baseUriProperty != null
          ? Optional.fromNullable(baseUriProperty.getURI())
          : Optional.<String>absent();
    } else {
      return Optional.absent();
    }
  }

  /**
   * Find the d2rq Server resource in the configuration model if present.
   *
   * @param model given configuration
   * @return {@link org.d2rq.vocab.D2RConfig#Server} resource
   */
  static Optional<Resource> findServerResource(final Model model) {
    return findSingleResourceOfType(model, D2RConfig.Server);
  }

  /**
   * Find the d2rq Database resource in the configuration model.
   *
   * @param model given configuration to search in
   * @return {@link org.d2rq.vocab.D2RQ#Database} resource
   */
  static Optional<Resource> findDatabaseResource(final Model model) {
    return findSingleResourceOfType(model, D2RQ.Database);
  }

  /**
   * Find a single resource with given rdf:type in the model if one is present.
   *
   * @param model model to search in
   * @param type  type of required resource
   * @return the resource if present
   * @throws java.lang.IllegalArgumentException if multiple resources with matching type are found
   */
  static Optional<Resource> findSingleResourceOfType(final Model model, final Resource type) {
    final ResIterator it = model.listResourcesWithProperty(RDF.type, type);
    final Optional<Resource> found = it.hasNext()
        ? Optional.of(it.nextResource())
        : Optional.<Resource>absent();
    if (found.isPresent() && it.hasNext()) {
      throw new IllegalArgumentException("found multiple <" + type + "> resources");
    }
    return found;
  }

  private final Model configuration;
  private final Mapping mapping;

  private D2rqSpec(final Model configuration) {
    this.configuration = configuration;
    mapping = new D2RQReader(this.configuration, getBaseResourceIri()).getMapping();
    mapping.configuration().setUseAllOptimizations(true);
  }

  /**
   * Create a d2rq-jena model from the wrapped mapping, using the given connection to a database.
   *
   * @param connection connection to the backing database
   * @return a jena model connected to the relational database via d2rq.
   */
  public Model compile(final SQLConnection connection) {
    final D2RQCompiler compiler = new D2RQCompiler(mapping);
    compiler.useConnection(connection);
    final CompiledMapping compiled = compiler.getResult();
    final GraphD2RQ graph = new GraphD2RQ(compiled);
    final Model model = ModelFactory.createModelForGraph(graph);
    model.withDefaultMappings(PrefixMapping.Extended);
    return model;
  }

  /**
   * The d2rq mapping definition, parsed from the wrapped configuration model.
   *
   * @return the d2rq mapping
   */
  public Mapping getMapping() {
    return mapping;
  }

  /**
   * Strip contextual information from the d2rq configuration model. Only mapping definitions and an
   * empty database resource remain.
   *
   * @return context free model
   */
  public Model getContextFreeModel() {
    final Model clone = ModelFactory.createDefaultModel().add(configuration);
    clone.setNsPrefixes(configuration.getNsPrefixMap());
    final Optional<Resource> database = findDatabaseResource(clone);
    if (database.isPresent()) {
      database.get().removeProperties();
      database.get().addProperty(RDF.type, D2RQ.Database);
    } else {
      clone.createResource(DEFAULT_BASE + "database", D2RQ.Database);
    }
    final Optional<Resource> server = findServerResource(clone);
    if (server.isPresent()) {
      server.get().removeProperties();
    }
    return clone;
  }

  /**
   * The IRI of the D2r:Server resource in the configuration.
   *
   * @return the resource identifier of this d2rq mapping
   */
  public String getIdentifier() {
    final Optional<Resource> serverResource = findServerResource(configuration);
    if (serverResource.isPresent() && serverResource.get().isURIResource()) {
      return serverResource.get().getURI();
    } else {
      throw new D2RQException("Missing <" + D2RConfig.Server + "> resource");
    }
  }

  /**
   * The absolute IRI used to resolve relative references in the source configuration model.
   *
   * @return base IRI of this configuration
   */
  public String getBaseResourceIri() {
    return findEmbeddedBaseUri(configuration).or(DEFAULT_BASE);
  }

  /**
   * The sparql timeout defined in this configuration
   *
   * @return Concrete timeout or {@link at.ac.univie.isc.asio.tool.TimeoutSpec#UNDEFINED}.
   */
  public TimeoutSpec getSparqlTimeout() {
    final Optional<Resource> serverResource = findServerResource(configuration);
    if (serverResource.isPresent() && serverResource.get().hasProperty(D2RConfig.sparqlTimeout)) {
      final Resource server = serverResource.get();
      final RDFNode value = server.getProperty(D2RConfig.sparqlTimeout).getObject();
      try {
        return TimeoutSpec.from(value.asLiteral().getLong(), TimeUnit.SECONDS);
      } catch (Exception cause) {
        throw new D2RQException(
            Pretty.format("Value for d2r:sparqlTimeout must be a numeric literal: '%s'", value),
            cause, D2RQException.MUST_BE_NUMERIC);
      }
    } else {
      return TimeoutSpec.undefined();
    }
  }

  /**
   * Whether this dataset supports federated sparql queries.
   *
   * @return true if federated queries are supported
   */
  public boolean isSupportingFederation() {
    final Optional<Resource> service =
        findSingleResourceOfType(configuration, SparqlServiceDescription.Service);
    return service.isPresent()
        && service.get().hasProperty(SparqlServiceDescription.feature, SparqlServiceDescription.BasicFederatedQuery);
  }
}
