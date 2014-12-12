package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.engine.Command;
import at.ac.univie.isc.asio.engine.Engine;
import at.ac.univie.isc.asio.engine.Language;
import at.ac.univie.isc.asio.engine.d2rq.D2rqSpec;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sql.JdbcSpec;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.tool.Resources;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.zaxxer.hikari.HikariDataSource;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Schema implements AutoCloseable {
  private final D2rqSpec d2rq;
  private final JenaEngine sparqlEngine;
  private final Model d2rqModel;

  private final JdbcSpec jdbc;
  private final JooqEngine sqlEngine;
  private final HikariDataSource jdbcPool;

  Schema(final D2rqSpec d2rq, final JenaEngine sparqlEngine, final Model d2rqModel, final JdbcSpec jdbc, final JooqEngine sqlEngine, final HikariDataSource jdbcPool) {
    this.d2rq = d2rq;
    this.sparqlEngine = sparqlEngine;
    this.d2rqModel = d2rqModel;
    this.jdbc = jdbc;
    this.sqlEngine = sqlEngine;
    this.jdbcPool = jdbcPool;
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
      throw new Command.Factory.LanguageNotSupported(language);
    }
  }

  // TODO : rethink ?
  public HikariDataSource getJdbcPool() {
    return jdbcPool;
  }

  public Set<Engine> engines() {
    return new HashSet<>(Arrays.asList(sqlEngine, sparqlEngine));
  }

  @Override
  public void close() {
    Resources.close(d2rqModel);
    Resources.close(jdbcPool);
  }
}
