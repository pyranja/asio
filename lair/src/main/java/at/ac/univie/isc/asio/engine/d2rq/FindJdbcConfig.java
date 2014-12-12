package at.ac.univie.isc.asio.engine.d2rq;

import at.ac.univie.isc.asio.engine.sql.JdbcSpec;
import at.ac.univie.isc.asio.tool.TimeoutSpec;
import org.d2rq.lang.D2RQMappingVisitor;
import org.d2rq.lang.Database;
import org.d2rq.lang.Mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Retrieve {@link at.ac.univie.isc.asio.engine.sql.JdbcSpec jdbc configurations} from a D2RQ
 * mapping.
 */
public final class FindJdbcConfig extends D2RQMappingVisitor.Default {
  public static FindJdbcConfig using(final TimeoutSpec sparqlTimeout) {
    return new FindJdbcConfig(sparqlTimeout);
  }

  private final TimeoutSpec sparqlTimeout;
  private final Set<JdbcSpec> specs;

  private FindJdbcConfig(final TimeoutSpec sparqlTimeout) {
    this.sparqlTimeout = sparqlTimeout;
    specs = new HashSet<>();
  }

  @Override
  public void visit(final Database database) {
    final JdbcSpec.Builder builder = JdbcSpec.connectTo(database.getJdbcURL()).use(sparqlTimeout);
    if (database.getJDBCDriver() != null) {
      builder.with(database.getJDBCDriver());
    }
    if (database.getPassword() != null) {
      builder.authenticateAs(database.getUsername(), database.getPassword());
    }
    specs.add(builder.complete());
  }

  public JdbcSpec findOneIn(final Mapping mapping) {
    mapping.accept(this);
    return single();
  }

  public Set<JdbcSpec> getSpecs() {
    return Collections.unmodifiableSet(specs);
  }

  public JdbcSpec single() {
    if (specs.isEmpty()) {
      throw new IllegalStateException("no jdbc properties found in d2rq mapping");
    }
    if (specs.size() > 1) {
      throw new IllegalStateException("more than one jdbc configuration found in d2r mapping");
    }
    return specs.iterator().next();
  }
}
