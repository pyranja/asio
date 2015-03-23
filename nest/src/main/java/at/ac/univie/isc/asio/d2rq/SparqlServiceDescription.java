package at.ac.univie.isc.asio.d2rq;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * RDF constants for the Sparql Service Description vocabulary.
 */
public final class SparqlServiceDescription {
  private SparqlServiceDescription() { /* no instances */ }

  /**
   * The sparql service description namespace.
   */
  public static final String NAMESPACE = "http://www.w3.org/ns/sparql-service-description#";

  private static final Model HOLDER = ModelFactory.createDefaultModel();

  public static final Resource Service = resource("Service");

  public static final Resource BasicFederatedQuery = resource("BasicFederatedQuery");

  public static final Property feature = property("feature");

  private static Resource resource(final String name) {
    return HOLDER.createResource(NAMESPACE + name);
  }

  private static Property property(final String name) {
    return HOLDER.createProperty(NAMESPACE, name);
  }
}
