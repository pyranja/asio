package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.tool.Pretty;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.d2rq.D2RQException;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.Mapping;
import org.d2rq.vocab.D2RConfig;

import java.util.concurrent.TimeUnit;

/**
 * Capture d2rq settings and mapping.
 */
public final class D2rqSpec {
  /**
   * Default base to resolve relative IRIs
   */
  static final String DEFAULT_BASE = "asio:///default/";

  /**
   * Find the embedded base resource IRI if present. Fall back to a default one.
   * @param model given configuration
   * @return base resource IRI embedded in model or {@link D2rqSpec#DEFAULT_BASE}.
   */
  static String findEmbeddedBaseUri(final Model model) {
    final Resource server = findServerResource(model);
    final Resource baseUriProperty = server.getPropertyResourceValue(D2RConfig.baseURI);
    return baseUriProperty != null ? baseUriProperty.getURI() : DEFAULT_BASE;
  }

  /**
   * Find the d2rq Server resource in the configuration model or create an empty one as fallback.
   * @param model given configuration
   * @return {@link org.d2rq.vocab.D2RConfig#Server} resource
   */
  static Resource findServerResource(final Model model) {
    final ResIterator it = model.listResourcesWithProperty(RDF.type, D2RConfig.Server);
    final Resource found = it.hasNext() ? it.nextResource() : model.createResource();
    if (it.hasNext()) {
      throw new IllegalArgumentException("found multiple <" + D2RConfig.Server + "> resources");
    }
    return found;
  }

  /**
   * Wrap the given rdf based d2rq configuration model.
   * @param configuration rdf model of settings
   * @return wrapper
   */
  public static D2rqSpec wrap(final Model configuration) {
    return new D2rqSpec(configuration);
  }

  private final Model configuration;
  private final Mapping mapping;

  private D2rqSpec(final Model configuration) {
    this.configuration = configuration;
    mapping = new D2RQReader(this.configuration, getBaseResourceIri()).getMapping();
    mapping.configuration().setUseAllOptimizations(true);
  }

  /**
   * The absolute IRI used to resolve relative references in the source configuration model.
   * @return base IRI of this configuration
   */
  public String getBaseResourceIri() {
    return findEmbeddedBaseUri(configuration);
  }

  /**
   * The sparql timeout defined in this configuration
   * @return Concrete timeout or {@link at.ac.univie.isc.asio.tool.TimeoutSpec#UNDEFINED}.
   */
  public TimeoutSpec getSparqlTimeout() {
    final Resource server = findServerResource(configuration);
    if (server.hasProperty(D2RConfig.sparqlTimeout)) {
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
   * The mapping of SQL tables to SPARQL concepts.
   * @return d2rq mapping definition
   */
  public Mapping getMapping() {
    return mapping;
  }
}
