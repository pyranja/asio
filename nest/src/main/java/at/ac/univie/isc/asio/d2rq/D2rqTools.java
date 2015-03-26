package at.ac.univie.isc.asio.d2rq;

import com.google.common.base.Optional;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import org.d2rq.CompiledMapping;
import org.d2rq.db.SQLConnection;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.D2RQCompiler;
import org.d2rq.lang.Mapping;
import org.d2rq.vocab.D2RConfig;

/**
 * Utility methods for working with d2rq.
 */
public final class D2rqTools {
  /**
   * Default base to resolve relative IRIs
   */
  public static final String DEFAULT_BASE = "asio:///default/";

  private D2rqTools() { /* no instances */ }

  /**
   * Find a single resource with given rdf:type in the model if one is present.
   *
   * @param model model to search in
   * @param type  type of required resource
   * @return the resource if present
   * @throws IllegalArgumentException if multiple resources with matching type are found
   */
  static Optional<Resource> findSingleOfType(final Model model, final Resource type) {
    final ResIterator it = model.listResourcesWithProperty(RDF.type, type);
    final Optional<Resource> found = it.hasNext()
        ? Optional.of(it.nextResource())
        : Optional.<Resource>absent();
    if (found.isPresent() && it.hasNext()) {
      throw new IllegalArgumentException("found multiple <" + type + "> resources");
    }
    return found;
  }

  /**
   * Find the embedded base resource IRI if present.
   *
   * @param model given configuration
   * @return base resource IRI embedded in model.
   */
  static Optional<String> findEmbeddedBaseUri(final Model model) {
    final Optional<Resource> server = findSingleOfType(model, D2RConfig.Server);
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
   * Create a d2rq-jena model from the given mapping, using the given connection to a database.
   * The supplied connection will override jdbc configuration in the mapping.
   * An {@link PrefixMapping#Extended extended set} of standard prefixes is added to the model.
   *
   * @param mapping    d2rq mapping that should be compiled
   * @param connection connection to the backing database
   * @return a jena model connected to the relational database via d2rq.
   */
  public static Model compile(final Mapping mapping, final SQLConnection connection) {
    final D2RQCompiler compiler = new D2RQCompiler(mapping);
    compiler.useConnection(connection);
    final CompiledMapping compiled = compiler.getResult();
    final GraphD2RQ graph = new GraphD2RQ(compiled);
    final Model model = ModelFactory.createModelForGraph(graph);
    model.withDefaultMappings(PrefixMapping.Extended);
    return model;
  }
}
