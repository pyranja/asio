package at.ac.univie.isc.asio.database;

import at.ac.univie.isc.asio.engine.d2rq.D2rqSpec;
import at.ac.univie.isc.asio.engine.d2rq.FindJdbcConfig;
import at.ac.univie.isc.asio.engine.sparql.JenaEngine;
import at.ac.univie.isc.asio.engine.sql.JdbcSpec;
import at.ac.univie.isc.asio.engine.sql.JooqEngine;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.d2rq.jena.GraphD2RQ;
import org.d2rq.lang.CompiledD2RQMapping;
import org.d2rq.lang.Mapping;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

/**
 * Produce {@code Schema}s taking into account global configuration properties.
 */
public final class SchemaFactory {

    private final Environment env;

    public SchemaFactory(final Environment env) {
        this.env = env;
    }

    /**
     * Create a {@code Schema} from d2rq configuration, including a SQL engine.
     * @param d2rq source configuration
     * @return initialized schema
     */
    public Schema fromD2rq(final D2rqSpec d2rq) {
        final TimeoutSpec timeout = d2rq.getSparqlTimeout().orIfUndefined(globalTimeout());
        final JdbcSpec jdbc = FindJdbcConfig.using(timeout).findOneIn(d2rq.getMapping());
        final Model d2rqModel = d2rModel(d2rq.getMapping());
        final JenaEngine sparqlEngine =
            JenaEngine.create(d2rqModel, timeout, isFederationAllowed());
        final HikariDataSource pool = connectionPool(jdbc);
        final JooqEngine sqlEngine = JooqEngine.create(pool, jdbc);
        return new Schema(d2rq, sparqlEngine, sqlEngine);
    }

    /**
     * Create an engine for {@link at.ac.univie.isc.asio.engine.Language#SPARQL}.
     * @param d2rq configuration from d2rq model
     * @return parametrized engine
     */
    public JenaEngine sparqlEngine(final D2rqSpec d2rq) {
        final Model model = d2rModel(d2rq.getMapping());
        final TimeoutSpec sparqlTimeout = d2rq.getSparqlTimeout().orIfUndefined(globalTimeout());
        return JenaEngine.create(model, sparqlTimeout, isFederationAllowed());
    }

    /**
     * Create an engine for {@link at.ac.univie.isc.asio.engine.Language#SQL}.
     * @param jdbc configuration of jdbc connection
     * @return parametrized engine
     */
    public JooqEngine sqlEngine(final JdbcSpec jdbc) {
        final HikariDataSource pool = connectionPool(jdbc);
        return JooqEngine.create(pool, jdbc);
    }

    private Model d2rModel(final Mapping mapping) {
        final CompiledD2RQMapping compiled = mapping.compile();
        final GraphD2RQ graph = new GraphD2RQ(compiled);
        final Model model = ModelFactory.createModelForGraph(graph);
        model.withDefaultMappings(PrefixMapping.Extended);
        return model;
    }

    private HikariDataSource connectionPool(final JdbcSpec jdbc) {
        final HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(concurrency());
        config.setDriverClassName(jdbc.getDriver());
        config.setJdbcUrl(jdbc.getUrl());
        config.setUsername(jdbc.getUsername());
        config.setPassword(jdbc.getPassword());
        config.setConnectionTimeout(jdbc.getTimeout().getAs(TimeUnit.MILLISECONDS, 0L));
        return new HikariDataSource(config);
    }

    private int concurrency() {
        return env.getProperty("asio.concurrency", Integer.class, 10);
    }

    private boolean isFederationAllowed() {
        return env.getProperty("asio.sparql.allowFederated", Boolean.class, Boolean.FALSE);
    }

    private TimeoutSpec globalTimeout() {
        Long timeout = env.getProperty("asio.timeout", Long.class, -1L);
        return TimeoutSpec.from(timeout, TimeUnit.SECONDS);
    }
}
