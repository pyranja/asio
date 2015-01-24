package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Language;
import at.ac.univie.isc.asio.engine.d2rq.D2rqSpec;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.tool.Resources;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Schema implements AutoCloseable {
  private final D2rqSpec d2rq;
  private final JenaEngine sparqlEngine;
  private final JooqEngine sqlEngine;

  Schema(final D2rqSpec d2rq, final JenaEngine sparqlEngine, final JooqEngine sqlEngine) {
    this.d2rq = d2rq;
    this.sparqlEngine = sparqlEngine;
    this.sqlEngine = sqlEngine;
  }

  public URI identifier() {
    return URI.create(d2rq.getBaseResourceIri());
  }

  public Engine engine(final Language language) {
    if (Language.SQL == language) {
      return sqlEngine;
    } else if (Language.SPARQL == language) {
      return sparqlEngine;
    } else {
      throw new Language.NotSupported(language);
    }
  }

  public JooqEngine.SchemaProvider relationalModel() {
    return sqlEngine.schemaProvider();
  }

  public Set<Engine> engines() {
    return new HashSet<Engine>(Arrays.asList(sqlEngine, sparqlEngine));
  }

  @Override
  public void close() {
    Resources.close(sparqlEngine);
    Resources.close(sqlEngine);
  }
}
