package at.ac.univie.isc.asio.d2rq;

import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import org.d2rq.lang.D2RQReader;
import org.d2rq.lang.Mapping;
import org.d2rq.vocab.D2RConfig;

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

  private final Model model;
  private final Mapping mapping;

  private D2rqConfigModel(final Model model) {
    this.model = model;
    mapping = readMapping(model);
  }

  private Mapping readMapping(final Model model) {
    final String base = D2rqTools.findEmbeddedBaseUri(model).or(D2rqTools.DEFAULT_BASE);
    final Mapping mapping = new D2RQReader(model, base).getMapping();
    mapping.configuration().setUseAllOptimizations(true);
    return mapping;
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
   * The d2rq mapping rules read from the wrapped configuration.
   *
   * @return the d2rq mapping
   */
  public Mapping getMapping() {
    return mapping;
  }

  /**
   * Maximum duration of operations on this dataset. Undefined if missing.
   *
   * @return timeout
   */
  public TimeoutSpec getTimeout() {
    final Resource server = requireServer();
    return Optional.fromNullable(server.getProperty(D2RConfig.sparqlTimeout))
        .transform(new Function<Statement, TimeoutSpec>() {
          @Nullable
          @Override
          public TimeoutSpec apply(final Statement input) {
            final RDFNode value = input.getObject();
            try {
              return TimeoutSpec.from(value.asLiteral().getLong(), TimeUnit.SECONDS);
            } catch (Exception cause) {
              throw new InvalidD2rqConfig(D2RConfig.sparqlTimeout,
                  "[" + value + "] is not numeric");
            }
          }
        }).or(TimeoutSpec.undefined());
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
