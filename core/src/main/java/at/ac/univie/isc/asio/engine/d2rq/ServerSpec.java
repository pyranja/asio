package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.config.TimeoutSpec;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.d2rq.D2RQException;
import org.d2rq.vocab.D2RConfig;
import org.d2rq.vocab.VocabularySummarizer;

import java.util.concurrent.TimeUnit;

/**
 * Imported functionality from {@code org.d2rq.ConfigLoader}, without dependencies on server code.
 */
public class ServerSpec {
  private String serverName = "asio-d2r";
  private String baseURI = "http://localhost/";
  private int port = 80;
  private boolean autoReloadMapping = true;
  private TimeoutSpec sparqlTimeout = TimeoutSpec.undefined();

  public ServerSpec(final Model model) {
    new VocabularySummarizer(D2RConfig.class).assertNoUndefinedTerms(model,
        D2RQException.CONFIG_UNKNOWN_PROPERTY,
        D2RQException.CONFIG_UNKNOWN_CLASS);
    init(findServerResource(model));
  }

  public String name() {
    return serverName;
  }

  public String baseURI() {
    return baseURI;
  }

  public int port() {
    return port;
  }

  public boolean isAutoReloadMapping() {
    return autoReloadMapping;
  }

  public TimeoutSpec timeout() {
    return sparqlTimeout;
  }

  private void init(final Resource server) {
    Statement s = server.getProperty(D2RConfig.baseURI);
    if (s != null) {
      this.baseURI = s.getResource().getURI();
    }
    s = server.getProperty(D2RConfig.port);
    if (s != null) {
      String value = s.getLiteral().getLexicalForm();
      try {
        this.port = Integer.parseInt(value);
      } catch (NumberFormatException ex) {
        throw new D2RQException("Illegal integer value '" + value + "' for d2r:port", D2RQException.MUST_BE_NUMERIC);
      }
    }
    s = server.getProperty(RDFS.label);
    if (s != null) {
      this.serverName = s.getString();
    }
    s = server.getProperty(D2RConfig.autoReloadMapping);
    if (s != null) {
      this.autoReloadMapping = s.getBoolean();
    }
    s = server.getProperty(D2RConfig.sparqlTimeout);
    if (s != null) {
      try {
        String value = s.getLiteral().getLexicalForm();
        sparqlTimeout = TimeoutSpec.from(Long.parseLong(value), TimeUnit.SECONDS);
      } catch (Exception ex) {
        throw new D2RQException("Value for d2r:sparqlTimeout must be a numeric literal: '" + s.getObject() + "'", D2RQException.MUST_BE_NUMERIC);
      }
    }
  }

  private Resource findServerResource(final Model model) {
    final ResIterator it = model.listResourcesWithProperty(RDF.type, D2RConfig.Server);
    return Iterators.getNext(it, model.createResource());
  }
}
